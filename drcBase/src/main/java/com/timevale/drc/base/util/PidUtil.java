package com.timevale.drc.base.util;

import java.lang.management.ManagementFactory;

public class PidUtil {

    static String pid;

    public static String getPID() {
        if (pid == null) {
            synchronized (PidUtil.class) {
                String name = ManagementFactory.getRuntimeMXBean().getName();
                pid = name.split("@")[0];
            }
        }
        return pid;
    }


}
