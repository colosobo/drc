package com.timevale.drc.worker.service.task.mysql.incr.support;

import com.alibaba.otter.canal.protocol.Message;
import com.timevale.drc.base.SinkFactory;
import com.timevale.drc.base.Transform;
import com.timevale.drc.base.dao.DrcDbConfigMapper;
import com.timevale.drc.base.dao.DrcSubTaskIncrMapper;
import com.timevale.drc.base.dao.DrcTaskRegisterTableMapper;
import com.timevale.drc.base.model.bo.DrcSubTaskIncrBO;
import com.timevale.drc.base.rocketmq.admin.MQAdminService;
import com.timevale.drc.base.util.DrcZkClient;
import com.timevale.drc.worker.service.WorkerServer;
import com.timevale.drc.worker.service.task.mysql.incr.MySqlIncrTransform;


/**
 * @author gwk_2
 * @date 2021/4/26 16:09
 */
public class CanalRowIncrTaskFactory extends BaseIncrTaskFactory<Message> {


    public CanalRowIncrTaskFactory(WorkerServer workerServer,
                                   DrcSubTaskIncrMapper drcSubTaskIncrMapper,
                                   DrcDbConfigMapper drcDbConfigMapper,
                                   DrcZkClient drcZkClient,
                                   MQAdminService mqAdminService,
                                   DrcTaskRegisterTableMapper drcTaskRegisterTableMapper, SinkFactory sinkFactory) {
        super(workerServer, drcSubTaskIncrMapper, drcDbConfigMapper, drcZkClient, mqAdminService, drcTaskRegisterTableMapper, sinkFactory);
    }

    @Override
    protected Transform<Message> getTransform() {
        return new MySqlIncrTransform<>();
    }

    @Override
    protected MessageHandler<Message> getMessageHandler(DrcSubTaskIncrBO drcSubTaskIncr) {
        return new CanalRowMessageHandler();
    }
}
