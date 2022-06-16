package com.timevale.drc.base.util;

/**
 * @author gwk_2
 * @date 2021/5/31 17:51
 */
public class TaskNameBuilder {

    public static String buildIncrName(String taskName) {
        return taskName + "_Incr";
    }

    public static String buildFullName(String taskName, String tableName, int num) {
        return taskName + "_full_" + tableName.trim() + "_" + num;
    }
}
