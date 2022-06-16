package com.timevale.drc.base.util;

/**
 * @author gwk_2
 * @date 2021/1/28 22:35
 * /middleware/drc
 */
public class ZkPathConstant {

    public static final String GROUP = System.getProperty("drc.root2.path", "drc");

    private static final String ROOT = "/middleware/" + GROUP;

    private static final String COORDINATOR = "/coordinator";

    public static String getCoordinator(String taskName) {
        return ROOT + COORDINATOR + "/" + taskName;
    }

}
