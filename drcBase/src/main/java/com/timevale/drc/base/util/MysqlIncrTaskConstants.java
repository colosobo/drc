package com.timevale.drc.base.util;

/**
 * @author gwk_2
 * @date 2021/1/28 23:22
 */
public class MysqlIncrTaskConstants {
    public static final String ROOT = "_DRC_WORKER_";

    /** 状态字段 */
    private static final String RUNNING_STATUS_PREFIX = ROOT + "RUNNING_STATUS_";
    /** 暂停时间. */
    private static final String PAUSE_TIME_PREFIX = ROOT + "PAUSE_TIME_";
    /** 暂存的消息数量, mq 消费会减1, canal 投递会加1 */
    private static final String STASH_MSG_NUM_PREFIX = ROOT + "STASH_MSG_NUM_";
    /** 最近接受消息的时间 */
    private static final String LAST_RECEIVED_TIME_PREFIX = ROOT + "LAST_RECEIVED_TIME_";
    /** 第一条数据的时间 */
    private static final String FIRST_DATA_PREFIX = ROOT + "FIRST_DATA_";

    /** 暂存中 */
    public static final String RUNNING_STATUS_STASHING = "STASHING";
    /** 回放中 */
    public static final String RUNNING_STATUS_REPLAYING = "REPLAYING";
    /** 回放时, 需要暂停写入到 暂存Sink 里(暂停暂存). */
    public static final String RUNNING_STATUS_STASH_PAUSED = "STASH_PAUSED";
    /** 直接 Sink. */
    public static final String RUNNING_STATUS_DIRECT = "DIRECT";

    /**
     * 默认的ddl执行超时时间
     */
    public static final int DEFAULT_DDL_TIMEOUT = 3600;

    public static String getRunningStatus(String taskName) {
        return RUNNING_STATUS_PREFIX + taskName;
    }

    public static String getPauseTime(String taskName) {
        return PAUSE_TIME_PREFIX + taskName;
    }

    public static String getStashMsgNum(String taskName) {
        return STASH_MSG_NUM_PREFIX + taskName;
    }

    public static String getLastReceivedTime(String taskName) {
        return LAST_RECEIVED_TIME_PREFIX + taskName;
    }

    public static String getFirstData(String taskName) {
        return FIRST_DATA_PREFIX + taskName;
    }


}
