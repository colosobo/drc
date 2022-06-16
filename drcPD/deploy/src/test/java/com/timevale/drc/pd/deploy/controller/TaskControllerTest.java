package com.timevale.drc.pd.deploy.controller;

import com.timevale.drc.base.sinkConfig.RocketSinkConfig;
import com.timevale.drc.pd.service.vo.DrcTaskVO;
import com.timevale.drc.pd.service.vo.MixTaskInput;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class TaskControllerTest {


    @Autowired
    TaskController taskController;

    @Test
    public void addMixTask() {

        MixTaskInput mixTaskInput = new MixTaskInput();
        mixTaskInput.setFullDbConfig(null);
        mixTaskInput.setIncrDbConfig(null);

        DrcTaskVO drcTaskVO = new DrcTaskVO();
        drcTaskVO.setTaskName("cxs");
        drcTaskVO.setDesc("cxs");
        drcTaskVO.setType(3);
        drcTaskVO.setQpsLimitConfig(100);
        drcTaskVO.setSinkType(3);
        drcTaskVO.setKafkaSinkConfig(null);
        drcTaskVO.setMySQLSinkConfig(null);

        RocketSinkConfig rocketSinkConfig = new RocketSinkConfig();
        rocketSinkConfig.setNameServer("192.168.1.1:9876");
        rocketSinkConfig.setTopic("cxs");
        rocketSinkConfig.setTag("");
        rocketSinkConfig.setMessageFormatType(1);

        drcTaskVO.setRocketSinkConfig(rocketSinkConfig);
        drcTaskVO.setCanalKafkaSinkConfig(null);

        mixTaskInput.setDrcTaskVO(drcTaskVO);
        mixTaskInput.setDrcSubTaskIncrVO(null);
        mixTaskInput.setFullTaskConfigVO(null);


        taskController.addMixTask(mixTaskInput);
    }
}
