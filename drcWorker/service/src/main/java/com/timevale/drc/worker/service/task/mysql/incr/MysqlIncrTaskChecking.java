package com.timevale.drc.worker.service.task.mysql.incr;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.timevale.drc.base.alarm.AlarmUtil;
import com.timevale.drc.base.log.TaskLog;
import com.timevale.drc.base.util.GlobalConfigUtil;
import com.timevale.drc.base.util.LogUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.timevale.drc.base.util.MysqlIncrTaskConstants.*;

/**
 *
 *
 * @author gwk_2
 * @date 2021/1/28 23:22
 */
@Slf4j
public class MysqlIncrTaskChecking {

    private static final Logger ALARM_LOG = LogUtil.getLogger("MysqlIncrTaskChecking");

    private final List<MysqlIncrTask<?>> taskCopyOnWriteArrayList = new CopyOnWriteArrayList<>();

    private final ScheduledExecutorService scheduled = new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder().setNameFormat("MysqlIncrTaskChecking-%d").build());

    private static final MysqlIncrTaskChecking INSTANCE = new MysqlIncrTaskChecking();

    public static MysqlIncrTaskChecking getInstance() {
        return INSTANCE;
    }

    public MysqlIncrTaskChecking() {
        init();
    }

    private void init() {
        // 遍历check全部增量task
        scheduled.scheduleWithFixedDelay(() -> {
            try {
                if (CollectionUtils.isEmpty(taskCopyOnWriteArrayList)) {
                    return;
                }
                for (MysqlIncrTask<?> mysqlIncrTask : taskCopyOnWriteArrayList) {
                    try {
                        taskChecking(mysqlIncrTask);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        String tips = mysqlIncrTask.getName() + " 增量任务检查异常, Exception = " + e.getMessage();
                        AlarmUtil.pushAlarm2Admin(tips);
                        mysqlIncrTask.getLog().error("增量任务检查异常, ", e);
                    }
                }
            } catch (Exception e) {
                ALARM_LOG.warn("增量任务检查异常, e:", e);
            }
        }, 5, 5, TimeUnit.SECONDS);
    }


    public void addTask(MysqlIncrTask<?> task) {
        if (taskCopyOnWriteArrayList.contains(task)) {
            return;
        }
        taskCopyOnWriteArrayList.add(task);
    }

    public boolean removeTask(MysqlIncrTask<?> task) {
        return taskCopyOnWriteArrayList.remove(task);
    }

    private synchronized <M> void taskChecking(MysqlIncrTask<M> task) {
        TaskLog taskLog = task.getLog();

        if (GlobalConfigUtil.getMySQLIncrMsgLogEnabled()) {
            taskLog.info("检查增量任务" + task.getName());
        }

        String runningStatus = task.getRunningStatus();

        if (StringUtils.isBlank(runningStatus)) {
            // 异常情况，记录日志，告警
            taskLog.warn("taskChecking, 任务运行状态为空，删除 checking 任务, TaskName = " + task.getName());
            // 移除该任务，不要一直告警
            removeTask(task);
            return;
        }
        if (GlobalConfigUtil.getMySQLIncrMsgLogEnabled()) {
            taskLog.info("当前状态 " + runningStatus);
        }

        // 先续期，保证不会失效
        task.setRunningStatus(runningStatus);

        if (runningStatus.equals(RUNNING_STATUS_REPLAYING)) {
            // 计算储存总数与取出数差值，小于一批，暂停储存
            long stashTotal = task.replay.getDiff();
            int stashDifferenceValue = GlobalConfigUtil.getStashDifferenceValue();
            taskLog.info("当前状态是 REPLAYING, 正在消费 MQ, 差值=" + stashTotal + ", 差值配置为:" + stashDifferenceValue + ", group = " + task.getBackUpConsumerGroup());
            if (stashTotal == -1 || stashTotal > stashDifferenceValue) {
                return;
            }
            // 状态改为暂停
            task.setRunningStatus(RUNNING_STATUS_STASH_PAUSED);
            task.paused.set(true);
            taskLog.info("增量任务消费进度快追上了，状态切换到回放暂停, TaskName = " + task.getName());
        } else if (runningStatus.equals(RUNNING_STATUS_STASH_PAUSED)) {
            long stashTotal = task.replay.getDiff();
            taskLog.info(String.format("当前状态是 STASH_PAUSED,  需要暂停 Canal 往 %s 写入. MQ, 差值= %s, group = %s",
                    task.runningSink.getClass().getCanonicalName(), stashTotal, task.getBackUpConsumerGroup()));
            // 改为暂停状态
            task.paused.set(true);
            // 计算储存总数与取出数差值，如果为0，说明储存的已全部消费完毕，开始直接投递
            if (stashTotal != 0) {
                return;
            }
            // sink替换
            task.runningSink = task.directSink;
            task.stagingSink.stop();
            // 状态改为直接投递
            task.setRunningStatus(RUNNING_STATUS_DIRECT);
            taskLog.info(String.format("增量任务回放完了!!!! 切换到 %s ，状态切换到直接投递, 取消暂停, TaskName = " + task.getName(), task.runningSink.getClass().getCanonicalName()));
            // 停掉mq consumer
            task.stopReplay();
            task.paused.set(false);
        } else {
            // 暂存状态/直接状态 就 取消暂停状态
            if (task.paused.get()) {
                taskLog.info("checking 暂停暂停设置为 false");
                task.paused.set(false);
            }

        }
    }

}
