package com.timevale.drc.base.util;


import com.beust.jcommander.internal.Lists;
import com.ctrip.framework.apollo.ConfigService;
import com.timevale.drc.base.serialize.JackSonUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author gwk_2
 * @date 2021/3/31 21:48
 */
public class GlobalConfigUtil {

    /**
     * 获取 mysql 增量同步的等待差值.
     *
     * @return
     */
    public static int getStashDifferenceValue() {
        return ConfigService.getAppConfig().getIntProperty("mysql.incr.task.reply.stash.difference.value", 300);
    }

    public static List<String> getBrokerList() {
        return Lists.newArrayList(ConfigService.getAppConfig().getProperty("rocketmq.broker.list", "pub-esign-broker-1,pub-esign-broker-0").split(","));
    }

    /**
     * 发送消息时, 是否打印日志
     */
    public static boolean getMySQLIncrMsgLogEnabled() {
        return ConfigService.getAppConfig().getBooleanProperty("msg.log.enabled", false);
    }

    /**
     * 限流器的总时间窗口长度(毫秒)
     */
    public static int getTotalWindowLengthInMs() {
        return ConfigService.getAppConfig().getIntProperty("limit.totalWindowLengthInMs", 60_000);
    }

    /**
     * 限流器的单个时间窗口长度(毫秒)
     */
    public static int getOneWindowLengthInMs() {
        return ConfigService.getAppConfig().getIntProperty("limit.oneWindowLengthInMs", 20);
    }

    /**
     * 是否打印 binlog  rowKey
     */
    public static boolean printBinlogRowKeyEnabled() {
        return ConfigService.getAppConfig().getBooleanProperty("global.print.binlog.rowKey.enabled", true);
    }

    /**
     * 是否打印 binlog  rowKey
     */
    public static boolean printBinlogDelayTime() {
        return ConfigService.getAppConfig().getBooleanProperty("global.print.binlog.delay.time.enabled", true);
    }

    public static int canalBatchSize() {
        return ConfigService.getAppConfig().getIntProperty("canal.batch.size", 50);
    }

    public static boolean kafkaAsyncEnable() {
        return ConfigService.getAppConfig().getBooleanProperty("kafka.async.enabled", false);
    }


    public static boolean mqAsyncEnabled() {
        return ConfigService.getAppConfig().getBooleanProperty("mq.async.enabled", false);
    }

    public static boolean ignoreMqExceptionEnabled() {
        return ConfigService.getAppConfig().getBooleanProperty("ignore.mq.exception.enabled", false);
    }

    public static int jdbcConnectionMaxPool() {
        return ConfigService.getAppConfig().getIntProperty("jdbc.connection.max.pool", 100);
    }


    public static String jdbcParams() {
        return ConfigService.getAppConfig().getProperty("jdbc.connection.params", "");
    }


    /**
     * key: tableName(推荐库+表，避免冲突)
     * value: 超时时间（单位：秒）
     */
    public static Map<String, Integer> getMysqlDDLSyncTimeoutMap() {
        String json = ConfigService.getAppConfig().getProperty("mysql.ddl.sync.timeout.config", "{}");
        return JackSonUtil.string2Obj(json, HashMap.class);
    }
}
