package com.timevale.drc.worker.service.task.mysql.incr;

import com.timevale.drc.base.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * redis 重试类, 当 redis 操作失败时, 会无限重试, 不做回滚处理.
 * 这里的假设条件:
 * 1. redis or 其他组件 失败,业务无法继续, 必须等待.
 * 2. redis or 其他组件 会快速恢复, 这种短时间的断开, 通常是扩容或者是缩容.
 * 3. 由于有告警, 可以通知到负责人, 负责人需要及时处理这种情况.
 *
 * @author gwk_2
 * @date 2021/4/1 19:33
 */
public class FailRetryUtil {

    private static final Logger ALARM_LOG = LoggerFactory.getLogger("alarm");


    public static void failRetry(Task task, Runnable runnable, FailCallBack failCallback) {
        doRetry(task, runnable, 0, failCallback);
    }

    private static void doRetry(Task task, Runnable runnable, int retry, FailCallBack failCallback) {
        try {
            runnable.run();
        } catch (Exception e) {
            ALARM_LOG.warn("发生了异常, 故障重试组件开始 sleep 3 秒, 然后递归重试, 重试次数=" + retry);
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
            if (failCallback != null) {
                failCallback.callback(e);
            }
            if (task != null && task.isRunning()) {
                doRetry(task, runnable, ++retry, failCallback);
            }
            throw new RuntimeException(e);
        }
    }

    public interface FailCallBack {

        void callback(Exception e);
    }
}
