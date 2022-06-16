package com.timevale.drc.worker.service.task.mysql.full;

import com.google.common.collect.Lists;
import com.timevale.drc.base.binlog.Binlog2JsonModel;
import com.timevale.drc.base.dao.DrcDbConfigMapper;
import com.timevale.drc.base.dao.DrcSubTaskFullConfigMapper;
import com.timevale.drc.base.dao.DrcSubTaskFullSliceDetailMapper;
import com.timevale.drc.base.model.DrcSubTaskFullSliceDetail;
import com.timevale.drc.base.sinkConfig.MySQLSinkConfig;
import com.timevale.drc.base.util.JdbcTemplateManager;
import com.timevale.drc.worker.deploy.Application;
import com.timevale.drc.worker.service.sink.mysql.MySqlBatchSink;
import com.timevale.drc.worker.service.sink.mysql.MySqlSink;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class MySqlFullExtractTest {

    private JdbcTemplateManager jdbcTemplateManager = new JdbcTemplateManager();
    MySqlFullExtract mySqlFullExtract;

    @Autowired
    private DrcSubTaskFullConfigMapper fullConfigMapper;
    @Autowired
    private DrcDbConfigMapper drcDbConfigMapper;
    @Autowired
    private DrcSubTaskFullSliceDetailMapper drcSubTaskFullSliceDetailMapper;


    @Test
    public void extract() {
        DrcSubTaskFullSliceDetail drcSubTaskFullSliceDetail = drcSubTaskFullSliceDetailMapper.selectByPrimaryKey(12495);
        mySqlFullExtract = new MySqlFullExtract(drcSubTaskFullSliceDetail, fullConfigMapper, drcDbConfigMapper, jdbcTemplateManager, 50, null);

        JdbcTemplate jdbcTemplate = new JdbcTemplateManager().get("localhost:3306", "root", "123456", "drc");

        jdbcTemplate.execute("delete from flow_info");

        MySqlBatchSink mySQLBatchSink = new MySqlBatchSink(jdbcTemplate, null);

        long s = System.currentTimeMillis(); // cost = 7599, b = 7312
        int a = 0;

        long b = 0;

        while (true) {
            if (a >= 10000) {
                break;
            }
            long h = System.currentTimeMillis();
            List<Binlog2JsonModel> extract = mySqlFullExtract.extract();
            if (extract == null) {
                break;
            }
            long k = System.currentTimeMillis();
            b += (k - h);

            mySQLBatchSink.sink(extract);
            String rowKey = extract.get(extract.size() - 1).getRowKey();
            Object value = extract.get(extract.size() - 1).getAfter().get(rowKey);
            drcSubTaskFullSliceDetail.setSliceCursor(value.toString());
            long e = System.currentTimeMillis();
            System.out.println("cost = " + (e - s));
            a += extract.size();
        }
        long e = System.currentTimeMillis();
        System.out.println("cost = " + (e - s) + ", b = " + b);

    }

    @Test //
    public void extract2() {
        DrcSubTaskFullSliceDetail drcSubTaskFullSliceDetail = drcSubTaskFullSliceDetailMapper.selectByPrimaryKey(12376);
        mySqlFullExtract = new MySqlFullExtract(drcSubTaskFullSliceDetail, fullConfigMapper, drcDbConfigMapper, jdbcTemplateManager, 50, null);

        JdbcTemplate jdbcTemplate = new JdbcTemplateManager().get("localhost:3306", "root", "123456", "drc");

        jdbcTemplate.execute("delete from flow_info");

        MySqlSink mySQLSink = new MySqlSink(jdbcTemplate, MySQLSinkConfig.builder()
                .tableName("flow_info")
                .build());

        long s = System.currentTimeMillis();
        int a = 0; // cost = 8580, b = 7153
        long b = 0;

        while (true) {
            if (a >= 10000) {
                break;
            }
            long h = System.currentTimeMillis();
            List<Binlog2JsonModel> extract = mySqlFullExtract.extract();
            if (extract == null) {
                break;
            }
            long k = System.currentTimeMillis();
            b += (k - h);

            for (Binlog2JsonModel model : extract) {
                mySQLSink.sink(Lists.newArrayList(model));
                String rowKey = model.getRowKey();
                Object value = model.getAfter().get(rowKey);
                drcSubTaskFullSliceDetail.setSliceCursor(value.toString());
            }
            long e = System.currentTimeMillis();
            System.out.println("cost = " + (e - s));
            a += extract.size();
        }
        long e = System.currentTimeMillis();
        System.out.println("cost = " + (e - s) + ", b = " + b);

    }
}
