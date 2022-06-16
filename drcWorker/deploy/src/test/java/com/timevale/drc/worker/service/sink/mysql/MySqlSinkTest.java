package com.timevale.drc.worker.service.sink.mysql;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.timevale.drc.base.binlog.Binlog2JsonModel;
import com.timevale.drc.base.sinkConfig.MySQLSinkConfig;
import com.timevale.drc.base.util.JdbcTemplateManager;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

public class MySqlSinkTest {

    @Test
    public void sink1() {

        JdbcTemplate jdbcTemplate = new JdbcTemplateManager().get("192.168.2.137:3306", "root", "root123", "xm");

        MySqlSink mySQLSink = new MySqlSink(jdbcTemplate, MySQLSinkConfig.builder()
                .tableName("sand_test")
                .build());

        Binlog2JsonModel binlog2JsonModel = new Binlog2JsonModel();
        binlog2JsonModel.setAction(Binlog2JsonModel.ACTION_INSERT);
        binlog2JsonModel.setDbName("xm");
        binlog2JsonModel.setTableName("sand_test");
        binlog2JsonModel.setRowKey("id");

        Map<String, Object> objectObjectHashMap = Maps.newHashMap();

        objectObjectHashMap.put("id", 3);
        objectObjectHashMap.put("sand", null);
        objectObjectHashMap.put("hex_sand", "17cf7e25720030a9");
        objectObjectHashMap.put("duo_sand", null);
        objectObjectHashMap.put("modify_time", "2021-03-29 15:03:18");
        objectObjectHashMap.put("create_time", "2021-03-29 15:03:18");

        binlog2JsonModel.setAfter(objectObjectHashMap);

        mySQLSink.sink(Lists.newArrayList(binlog2JsonModel));
    }

    @Test
    public void sink2() {

        JdbcTemplate jdbcTemplate = new JdbcTemplateManager().get("192.168.2.137:3306", "root", "root123", "xm");

        MySqlSink mySQLSink = new MySqlSink(jdbcTemplate, MySQLSinkConfig.builder()
                .tableName("sand_test")
                .build());

        Binlog2JsonModel binlog2JsonModel = new Binlog2JsonModel();
        binlog2JsonModel.setAction(Binlog2JsonModel.ACTION_UPDATE);
        binlog2JsonModel.setDbName("xm");
        binlog2JsonModel.setTableName("sand_test");
        binlog2JsonModel.setRowKey("id");

        Map<String, Object> objectObjectHashMap = Maps.newHashMap();

        objectObjectHashMap.put("id", 1);
        objectObjectHashMap.put("sand", 1715728682342953128L);
        objectObjectHashMap.put("hex_sand", "17cf7e25720030a9");
        objectObjectHashMap.put("duo_sand", "12121");
        objectObjectHashMap.put("modify_time", "2021-03-29 15:03:19");
        objectObjectHashMap.put("create_time", "2021-03-29 15:03:18");

        binlog2JsonModel.setAfter(objectObjectHashMap);


        mySQLSink.sink(Lists.newArrayList(binlog2JsonModel));
    }

    @Test
    public void sink3() {

        JdbcTemplate jdbcTemplate = new JdbcTemplateManager().get("192.168.2.137:3306", "root", "root123", "xm");

        MySqlSink mySQLSink = new MySqlSink(jdbcTemplate, MySQLSinkConfig.builder()
                .tableName("sand_test")
                .build());

        Binlog2JsonModel binlog2JsonModel = new Binlog2JsonModel();
        binlog2JsonModel.setAction(Binlog2JsonModel.ACTION_DELETE);
        binlog2JsonModel.setDbName("xm");
        binlog2JsonModel.setTableName("sand_test");
        binlog2JsonModel.setRowKey("id");

        Map<String, Object> objectObjectHashMap = Maps.newHashMap();

        objectObjectHashMap.put("id", 1);
        objectObjectHashMap.put("sand", 1715728682342953128L);
        objectObjectHashMap.put("hex_sand", "17cf7e25720030a9");
        objectObjectHashMap.put("duo_sand", "1fjru4lp00c5a");
        objectObjectHashMap.put("modify_time", "2021-03-29 15:03:18");
        objectObjectHashMap.put("create_time", "2021-03-29 15:03:18");

        binlog2JsonModel.setAfter(objectObjectHashMap);
        binlog2JsonModel.setBefore(objectObjectHashMap);


        mySQLSink.sink(Lists.newArrayList(binlog2JsonModel));
    }
}
