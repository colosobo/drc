package com.timevale.drc.base;

/**
 * @author gwk_2
 * @date 2021/4/13 17:00
 * @description
 * @see BaseTask#TYPE_INCR_TASK
 * @see BaseTask#TYPE_FULL_TASK
 * @see BaseTask#TYPE_MIX_TASK
 */
public enum TaskTypeEnum {
    /** test */
    UN_KNOW(0, "未知"),
    MYSQL_INCR_TASK(1, "MYSQL binlog 增量任务"),
    MYSQL_FULL_TASK(2, "MYSQL 全量任务"),
    MYSQL_MIX_TASK(3, "MYSQL 增量+全量混合任务"),
    MYSQL_DATABASE_MIX_TASK(4, "MYSQL database 级别增量+全量混合任务");

    public int code;
    public String desc;

    TaskTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TaskTypeEnum conv(int code) {
        for (TaskTypeEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}
