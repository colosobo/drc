package com.timevale.drc.pd.service.heartbeat;


import com.timevale.drc.base.TaskStateEnum;
import com.timevale.drc.base.metrics.TaskMetricsModel;
import com.timevale.drc.base.model.BaseTaskModel;
import com.timevale.drc.pd.facade.api.PdService;
import com.timevale.drc.pd.service.PDServer;
import com.timevale.drc.pd.service.TaskDbOperator;
import com.timevale.drc.pd.service.stat.TaskMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author gwk_2
 * @date 2021/1/28 22:42
 */
@Slf4j
@Service
public class PdServiceRpcServer implements PdService {

    @Autowired
    private PDServer pdServer;

    @Autowired
    private TaskDbOperator taskDbOperator;

    @Override
    public String unRegister(String taskName) {
        log.info("rpc 调用, taskName = {}", taskName);
        pdServer.stopTask(taskName, "unRegister");
        return "success";
    }

    @Override
    public String restart(String taskName) {
        try {
            BaseTaskModel model = taskDbOperator.lookup(taskName);
            if (model.getState() == TaskStateEnum.HAND_STOP.code) {
                return "no";
            }

            pdServer.stopTask(taskName, "restart");
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
            pdServer.startTask(taskName);
        } catch (Exception e) {
            log.error("pd 接口重启失败.", e);
            return "fail";
        }
        return "success";
    }

    @Override
    public String uploadTaskMetrics(Map<String, TaskMetricsModel> data) {
        for (Map.Entry<String, TaskMetricsModel> e : data.entrySet()) {
            TaskMetricsService.put(e.getKey(), e.getValue().getQps());
        }
        return "success";
    }

}
