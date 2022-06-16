package com.timevale.drc.base;

import com.timevale.drc.base.util.LogUtil;
import org.apache.log4j.Logger;
import org.junit.Test;

public class LogUtilTest {

    @Test
    public void log() {
        for (int i = 0; i < 10; i++) {
            Logger logUtilTestLogger = LogUtil.getLogger("LogUtilTestLogger");
            logUtilTestLogger.info("info log");
        }

        String logUtilTestLogger = LogUtil.getLogText("LogUtilTestLogger", 118);
        System.out.println(logUtilTestLogger);

        LogUtil.deleteLogger("LogUtilTestLogger");
    }



}
