package com.timevale.drc.worker.service.canal;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.common.collect.Lists;
import com.timevale.drc.base.binlog.Binlog2JsonModel;
import com.timevale.drc.base.util.DrcZkClient;
import com.timevale.drc.base.util.GlobalConfigUtil;
import com.timevale.drc.worker.deploy.Application;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class MyCanalServerTest {

    DrcCanalServer serverInstance;

    String dest = "flow_info_test163_Incr";

    @Autowired
    private DrcZkClient drcZkClient;

    @Before
    public void b() throws IOException {
        Properties properties = new Properties();
        properties.load(MyCanalServerTest.class.getClassLoader().getResourceAsStream("testCanal.properties"));
        serverInstance = DrcCanalServer.getInstanceWithStart("192.168.1.1:2181", drcZkClient, null);
        serverInstance.startInstance(dest, properties, null);
    }

    @Test
    public void start() throws IOException {


        long start = System.currentTimeMillis();

        long id;
        while (true) {
            if (TimeUnit.SECONDS.toMillis(1000) + start < System.currentTimeMillis()) {
                break;
            }
            Message msg = serverInstance.getWithoutAck(50, dest);
            id = msg.getId();
            if (id == -1) {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                continue;
            }

            List<Binlog2JsonModel> handler = handler(msg.getEntries());
            serverInstance.ack(id, dest);
            if (handler.size() == 0) {
                continue;
            }
            System.out.println(handler);
            break;
        }
        System.out.println(serverInstance.isStart(dest));
        serverInstance.stopServer();
        System.out.println(serverInstance.isStart(dest));

        System.out.println("start .....");
        dest = dest + ThreadLocalRandom.current().nextInt(1000000);
        b();
        start();
    }

    private void handleAfter(Binlog2JsonModel binlog2JsonModel, CanalEntry.RowData rowData) {
        ColumnsKey columnsKey = getColumnsKey(rowData.getAfterColumnsList());
        binlog2JsonModel.setAfter(columnsKey.getColumns());
        binlog2JsonModel.setRowKey(columnsKey.getKey());
    }

    private void handleBefore(Binlog2JsonModel binlog2JsonModel, CanalEntry.RowData rowData) {
        ColumnsKey columnsKey = getColumnsKey(rowData.getBeforeColumnsList());
        binlog2JsonModel.setBefore(columnsKey.getColumns());
        binlog2JsonModel.setRowKey(columnsKey.getKey());
    }

    private ColumnsKey getColumnsKey(List<CanalEntry.Column> columns) {
        ColumnsKey columnsKey = new ColumnsKey();
        Map<String, Object> map = new HashMap<>();
        for (CanalEntry.Column column : columns) {
            if (column.getIsKey()) {
                columnsKey.setKey(column.getName());
            }
            map.put(column.getName(), column.getValue());
        }
        columnsKey.setColumns(map);
        return columnsKey;
    }

    @Data
    static class ColumnsKey {
        Map<String, Object> columns;
        String key;
    }

    private void logDelay(CanalEntry.Entry entry) {
        long executeTime = entry.getHeader().getExecuteTime();
        long delay = System.currentTimeMillis() - executeTime;
        if (delay > 2000) {
            if (GlobalConfigUtil.printBinlogDelayTime()) {
                String tip = String.format("binlog 延迟时间, %s 毫秒, TableName=%s", delay, entry.getHeader().getTableName());
                log.info(tip);
            }
        }
    }

    private List<Binlog2JsonModel> handler(List<CanalEntry.Entry> entrys) {
        List<Binlog2JsonModel> result = Lists.newArrayList();
        for (CanalEntry.Entry entry : entrys) {
            // 事物，心跳直接跳过
            if (entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONBEGIN
                    || entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONEND
                    || entry.getEntryType() == CanalEntry.EntryType.HEARTBEAT) {
                continue;
            }

            // 打印延迟
            logDelay(entry);

            // 解析rowChange
            CanalEntry.RowChange rowChange;
            try {
                rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            } catch (Exception e) {
                throw new RuntimeException("ERROR parse rowChange, entry:" + entry.toString());
            }

            // 解析eventType
            CanalEntry.EventType eventType = rowChange.getEventType();

            // 查询直接跳过
            if (eventType == CanalEntry.EventType.QUERY || rowChange.getIsDdl()) {
                continue;
            }

            // 变更数据为空，直接跳过
            if (CollectionUtils.isEmpty(rowChange.getRowDatasList())) {
                continue;
            }

            // 转化为binlog2JsonModel
            Binlog2JsonModel binlog2JsonModel = new Binlog2JsonModel();
            binlog2JsonModel.setDbName(entry.getHeader().getSchemaName());
            binlog2JsonModel.setTableName(entry.getHeader().getTableName());

            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                if (eventType == CanalEntry.EventType.DELETE) {
                    binlog2JsonModel.setAction(Binlog2JsonModel.ACTION_DELETE);
                    handleBefore(binlog2JsonModel, rowData);
                } else if (eventType == CanalEntry.EventType.INSERT) {
                    binlog2JsonModel.setAction(Binlog2JsonModel.ACTION_INSERT);
                    handleAfter(binlog2JsonModel, rowData);
                } else {
                    binlog2JsonModel.setAction(Binlog2JsonModel.ACTION_UPDATE);
                    handleBefore(binlog2JsonModel, rowData);
                    handleAfter(binlog2JsonModel, rowData);
                }
            }

            if (GlobalConfigUtil.printBinlogRowKeyEnabled()) {
                String rowKey = binlog2JsonModel.getRowKey();
                if (binlog2JsonModel.getAfter() != null) {
                    log.info("After rowKey = " + binlog2JsonModel.getAfter().get(rowKey));
                }
                if (binlog2JsonModel.getBefore() != null) {
                    log.info("Before rowKey = " + binlog2JsonModel.getBefore().get(rowKey));
                }
            }
            result.add(binlog2JsonModel);
        }
        return result;
    }

    @Test
    public void stop() {
    }

    @Test
    public void testStart() {
    }

    @Test
    public void testStop() {
    }

    @Test
    public void getWithoutAck() {
    }

    @Test
    public void add() {
    }
}
