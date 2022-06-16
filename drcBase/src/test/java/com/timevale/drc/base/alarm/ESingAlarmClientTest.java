package com.timevale.drc.base.alarm;

import com.google.common.collect.Lists;
import org.junit.Test;

public class ESingAlarmClientTest {

    @Test
    public void alarm() throws Exception {
        AlarmClient eSingAlarmService = new AlarmClient(null, "test");

        eSingAlarmService.alarm("ut11111 test", Lists.newArrayList("xuanmie"));
    }
}
