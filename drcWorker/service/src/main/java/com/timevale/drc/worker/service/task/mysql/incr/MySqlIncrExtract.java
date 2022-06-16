package com.timevale.drc.worker.service.task.mysql.incr;

import com.alibaba.otter.canal.protocol.Message;
import com.google.common.collect.Lists;
import com.timevale.drc.base.Extract;
import com.timevale.drc.base.log.TaskLog;
import com.timevale.drc.base.metrics.DefaultTaskMetrics;
import com.timevale.drc.base.util.GlobalConfigUtil;
import com.timevale.drc.worker.service.canal.DrcCanalServer;
import com.timevale.drc.worker.service.task.mysql.incr.support.MessageHandler;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * 提取 mysql 全量数据.
 *
 * @author gwk_2
 */
@Getter
public class MySqlIncrExtract<M> implements Extract<List<M>> {

    private final MysqlIncrTask<M> task;

    private final TaskLog taskLog;
    private final DrcCanalServer drcCanalServer;
    private final MessageHandler<M> messageHandler;
    private long batchId;
    private boolean first;
    private final DefaultTaskMetrics taskMetrics;

    public MySqlIncrExtract(MysqlIncrTask<M> task, DrcCanalServer drcCanalServer, MessageHandler<M> messageHandler, DefaultTaskMetrics taskMetrics) {
        this.task = task;
        this.drcCanalServer = drcCanalServer;
        this.taskLog = task.getLog();
        this.messageHandler = messageHandler;
        this.taskMetrics = taskMetrics;
        this.first = true;
    }

    @Override
    public List<M> extract() {
        synchronized (this) {
            Message message = null;
            try {
                boolean start = drcCanalServer.isStart(task.getName());
                if (!start) {
                    drcCanalServer.startInstance(task.getName(), task.getProperties(), taskLog);
                    LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                }
                message = drcCanalServer.getWithoutAck(GlobalConfigUtil.canalBatchSize(), task.getName());
            } catch (Exception e) {
                taskLog.error(e.getMessage(), e);
            }
            if (message == null) {
                batchId = -1;
                return Lists.newArrayList();
            }

            batchId = message.getId();

            try {
                if (batchId == -1) {
                    sleep();
                    return Lists.newArrayList();
                }

                List<M> msg = messageHandler.handler(message);
                //taskMetrics.stat(message.getEntries().size());

                if (first) {
                    taskLog.info("msg = " + msg.size() + ", 收到 binlog 日志, size = " + message.getEntries().size());
                    first = false;
                }

                return msg;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    @Override
    public void ack() {
        if (batchId != -1) {
            drcCanalServer.ack(batchId, task.getName());
            batchId = -1;
        }
    }

    private void sleep() {
        // 歇会
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            // ignore
        }
    }


}
