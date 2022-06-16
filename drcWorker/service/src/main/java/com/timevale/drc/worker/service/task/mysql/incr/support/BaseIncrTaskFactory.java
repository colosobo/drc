package com.timevale.drc.worker.service.task.mysql.incr.support;

import com.timevale.drc.base.Sink;
import com.timevale.drc.base.SinkFactory;
import com.timevale.drc.base.Transform;
import com.timevale.drc.base.dao.DrcDbConfigMapper;
import com.timevale.drc.base.dao.DrcSubTaskIncrMapper;
import com.timevale.drc.base.dao.DrcTaskMapper;
import com.timevale.drc.base.dao.DrcTaskRegisterTableMapper;
import com.timevale.drc.base.model.DrcTask;
import com.timevale.drc.base.model.bo.DrcSubTaskIncrBO;
import com.timevale.drc.base.rocketmq.admin.MQAdminService;
import com.timevale.drc.base.sinkConfig.SinkConfig;
import com.timevale.drc.base.util.DrcZkClient;
import com.timevale.drc.worker.service.WorkerServer;
import com.timevale.drc.worker.service.canal.support.DrcLogAlarmHandler;
import com.timevale.drc.worker.service.task.Coordinator;
import com.timevale.drc.worker.service.task.mysql.incr.MysqlIncrTask;
import org.redisson.api.RRateLimiter;

import java.util.Properties;

import static com.timevale.drc.base.TaskTypeEnum.MYSQL_INCR_TASK;

/**
 *
 * @author gwk_2
 * @date 2021/4/26 16:09
 */
public abstract class BaseIncrTaskFactory<T> {

    protected WorkerServer workerServer;
    protected DrcSubTaskIncrMapper drcSubTaskIncrMapper;
    protected DrcDbConfigMapper drcDbConfigMapper;
    protected DrcZkClient drcZkClient;
    protected MQAdminService mqAdminService;
    protected DrcTaskRegisterTableMapper drcTaskRegisterTableMapper;
    protected SinkFactory sinkFactory;

    public BaseIncrTaskFactory(WorkerServer workerServer,
                               DrcSubTaskIncrMapper drcSubTaskIncrMapper,
                               DrcDbConfigMapper drcDbConfigMapper,
                               DrcZkClient drcZkClient,
                               MQAdminService mqAdminService,
                               DrcTaskRegisterTableMapper drcTaskRegisterTableMapper, SinkFactory sinkFactory) {
        this.workerServer = workerServer;
        this.drcSubTaskIncrMapper = drcSubTaskIncrMapper;
        this.drcDbConfigMapper = drcDbConfigMapper;
        this.drcZkClient = drcZkClient;
        this.mqAdminService = mqAdminService;
        this.drcTaskRegisterTableMapper = drcTaskRegisterTableMapper;
        this.sinkFactory = sinkFactory;
    }


    public MysqlIncrTask<T> create(SinkConfig sinkConfig, String drcZkAddr, DrcTask parentTask,
                                   Coordinator coordinator, Properties properties, DrcSubTaskIncrBO drcSubTaskIncrBO,
                                   RRateLimiter rateLimiter, DrcTaskMapper drcTaskMapper, DrcLogAlarmHandler drcLogAlarmHandler) {

        return new MysqlIncrTask<>(
                getMessageHandler(drcSubTaskIncrBO),
                drcZkAddr,
                drcSubTaskIncrBO,
                parentTask,
                drcSubTaskIncrMapper,
                drcDbConfigMapper,
                getSink(sinkConfig),
                mqAdminService,
                drcZkClient,
                coordinator,
                properties,
                rateLimiter,
                drcTaskMapper, getTransform(), drcLogAlarmHandler);
    }

    protected abstract Transform<T> getTransform();

    protected Sink<T> getSink(SinkConfig sinkConfig) {
        return sinkFactory.create(sinkConfig.taskType(MYSQL_INCR_TASK));
    }
    protected abstract MessageHandler<T> getMessageHandler(DrcSubTaskIncrBO drcSubTaskIncr);
}
