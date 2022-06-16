package com.timevale.drc.worker.service.canal.support;

import com.alibaba.otter.canal.common.AbstractCanalLifeCycle;
import com.alibaba.otter.canal.common.alarm.CanalAlarmHandler;
import com.alibaba.otter.canal.common.alarm.LogAlarmHandler;
import com.timevale.drc.base.log.TaskLog;
import com.timevale.drc.worker.service.RestartController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author gwk_2
 * @date 2021/4/15 11:05
 * @see LogAlarmHandler
 */
@Slf4j
@Component
public class DrcLogAlarmHandler extends AbstractCanalLifeCycle implements CanalAlarmHandler {

    private static final String ERROR1 = "master has purged binary logs containing GTIDs that the slave requires.";

    private static final String ERROR2 = "but the master has purged binary logs containing GTIDs that the slave requires";

    private static final String ERROR3 = "show master status";

    private static final Map<String, TaskLog> TASK_LOG_HASH_MAP = new HashMap<>();

    public static final Map<String, Boolean> NEED_DELETE_MAP = new ConcurrentHashMap<>();

    @Override
    public void sendAlarm(String destination, String msg) {
        log.error("DrcLogAlarmHandler canal Server 告警, 准备重启. destination:{}, msg= {}", destination, msg);

        TaskLog taskLog = TASK_LOG_HASH_MAP.get(destination);
        if (taskLog != null) {
            taskLog.info("canal server 异常, 准备重启, msg=" + msg);
        }

        if (msg.contains(ERROR3)) {
            if (taskLog != null) {
                taskLog.warn(msg);
            } else {
                log.warn(msg);
            }
            return;
        }

        // binlog 丢了. 需要删除 zk 位点, 否则会报错.
        if (msg.contains(ERROR1) || msg.contains(ERROR2)) {
            NEED_DELETE_MAP.put(destination, true);
        }
        // 重启.
        RestartController.putRestartTask(destination);

    }

    public void saveLog(String dest, TaskLog logger) {
        if (logger != null && dest != null) {
            TASK_LOG_HASH_MAP.put(dest, logger);
        }
    }

    public void remove(String destination) {
        TASK_LOG_HASH_MAP.remove(destination);
    }
}
