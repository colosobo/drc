package com.timevale.drc.worker.service.task.mysql.incr.support;

import com.timevale.drc.base.SinkFactory;
import com.timevale.drc.base.Transform;
import com.timevale.drc.base.binlog.Binlog2JsonModel;
import com.timevale.drc.base.dao.DrcDbConfigMapper;
import com.timevale.drc.base.dao.DrcSubTaskIncrMapper;
import com.timevale.drc.base.dao.DrcTaskRegisterTableMapper;
import com.timevale.drc.base.log.ApacheTaskLog;
import com.timevale.drc.base.model.bo.DrcSubTaskIncrBO;
import com.timevale.drc.base.rocketmq.admin.MQAdminService;
import com.timevale.drc.base.util.DrcZkClient;
import com.timevale.drc.worker.service.WorkerServer;
import com.timevale.drc.worker.service.task.mysql.incr.MySqlIncrDrcModelTransform;

/**
 * @author gwk_2
 * @date 2021/4/26 15:54
 */
public class Binlog2JsonModelIncrTaskFactory extends BaseIncrTaskFactory<Binlog2JsonModel> {

    public Binlog2JsonModelIncrTaskFactory(WorkerServer workerServer,
                                           DrcSubTaskIncrMapper drcSubTaskIncrMapper,
                                           DrcDbConfigMapper drcDbConfigMapper,
                                           DrcZkClient drcZkClient,
                                           MQAdminService mqAdminService,
                                           DrcTaskRegisterTableMapper drcTaskRegisterTableMapper, SinkFactory sinkFactory) {
        super(workerServer, drcSubTaskIncrMapper, drcDbConfigMapper, drcZkClient, mqAdminService, drcTaskRegisterTableMapper, sinkFactory);
    }

    @Override
    protected Transform<Binlog2JsonModel> getTransform() {
        return new MySqlIncrDrcModelTransform();
    }

    @Override
    protected MessageHandler<Binlog2JsonModel> getMessageHandler(DrcSubTaskIncrBO drcSubTaskIncr) {
        String taskName = drcSubTaskIncr.getSubTaskName();
        return new Binlog2JsonModelMessageHandler(
                new ApacheTaskLog(taskName),
                drcSubTaskIncr.getDrcSubTaskIncrExt().getSupportDDLSync(),
                drcSubTaskIncr.getDrcSubTaskIncrExt().getDDLSyncFilterDML(),
                taskName);
    }
}
