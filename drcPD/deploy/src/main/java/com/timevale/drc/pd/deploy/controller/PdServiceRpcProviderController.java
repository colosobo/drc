package com.timevale.drc.pd.deploy.controller;

import com.timevale.drc.base.metrics.TaskMetricsModel;
import com.timevale.drc.pd.facade.api.PdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author gwk_2
 * @date 2022/1/18 01:26
 * @description
 */
@Slf4j
@RestController
@RequestMapping("/")
public class PdServiceRpcProviderController {

    @Autowired
    private PdService pdService;

    @PostMapping("/unRegister/taskName")
    public String unRegister(@RequestBody String taskName) {
        return pdService.unRegister(taskName);
    }

    @PostMapping("/restart/taskName")
    public String restart(@RequestBody String taskName) {
        return pdService.restart(taskName);
    }

    @PostMapping("/uploadTaskMetrics")
    public String uploadTaskMetrics(@RequestBody Map<String, TaskMetricsModel> data) {
        return pdService.uploadTaskMetrics(data);
    }
}
