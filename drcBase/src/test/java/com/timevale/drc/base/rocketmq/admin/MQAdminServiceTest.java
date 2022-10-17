package com.timevale.drc.base.rocketmq.admin;

import org.junit.Test;

public class MQAdminServiceTest {

    @Test
    public void getDiffTotal() {
        MQAdminService mqAdminService = new MQAdminService();
        mqAdminService.setNameSrvAddr("192.168.1.1:9876");
        mqAdminService.initAddrList();

        long drc_mq_replay_push_log_321_incr = mqAdminService.getDiffTotal("DRC_MQ_REPLAY_push_log_321_Incr");
        System.out.println(drc_mq_replay_push_log_321_incr);
    }
}
