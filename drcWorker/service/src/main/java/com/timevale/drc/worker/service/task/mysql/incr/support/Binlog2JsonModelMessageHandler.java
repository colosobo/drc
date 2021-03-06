package com.timevale.drc.worker.service.task.mysql.incr.support;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.timevale.drc.base.binlog.Binlog2JsonModel;
import com.timevale.drc.base.log.TaskLog;
import com.timevale.drc.base.metrics.TimeFactory;
import com.timevale.drc.base.util.GlobalConfigUtil;
import com.timevale.drc.base.util.MysqlIncrTaskConstants;
import com.timevale.drc.worker.service.task.mysql.util.DrcCanalEntryParseUtil;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;


public class Binlog2JsonModelMessageHandler implements MessageHandler<Binlog2JsonModel> {
    private static Set<CanalEntry.EntryType> excludeSet =
            Sets.newHashSet(CanalEntry.EntryType.TRANSACTIONBEGIN,
                    CanalEntry.EntryType.TRANSACTIONEND,
                    CanalEntry.EntryType.HEARTBEAT);

    private boolean supportDDLSync;
    private boolean DDLSyncFilterDML;
    private TaskLog log;
    protected String lastReceivedTimeKey;
    private Gson gson = new Gson();

    private long delayCounter = 0;
    private long delayTotal = 0;


    public Binlog2JsonModelMessageHandler(TaskLog log, Boolean supportDDLSync, Boolean DDLSyncFilterDML, String taskName) {
        this.log = log;
        this.supportDDLSync = BooleanUtils.isTrue(supportDDLSync);
        this.DDLSyncFilterDML = BooleanUtils.isTrue(DDLSyncFilterDML);
        this.lastReceivedTimeKey = MysqlIncrTaskConstants.getLastReceivedTime(taskName);
    }

    @Override
    public List<Binlog2JsonModel> handler(Message message) {
        if (!message.isRaw() && message.getEntries().size() > 0) {
            return handleEntry(message.getEntries());
        }
        return null;
    }

    @Override
    public Binlog2JsonModel convFromJson(String json) {
        return gson.fromJson(json, Binlog2JsonModel.class);
    }


    private List<Binlog2JsonModel> handleEntry(List<CanalEntry.Entry> entrys) {
        List<Binlog2JsonModel> totalEntryResult = Lists.newArrayList();

        // key??????????????????i???value????????????entry
        Map<Integer,CanalEntry.Entry> lastEntryMap = new HashMap<>();

        for (int i = 0; i < entrys.size(); i++) {
            CanalEntry.Entry entry = entrys.get(i);

            // ???????????????????????????
            if (excludeSet.contains(entry.getEntryType())) {
                continue;
            }

            // ??????rowChange
            CanalEntry.RowChange rowChange = DrcCanalEntryParseUtil.parseRowChange(entry);

            List<Binlog2JsonModel> singleEntryResult = handleCanalEntry(entry,rowChange);

            if(DrcCanalEntryParseUtil.isRowsQueryLogEventEntry(rowChange)){
                // RowsQueryLogEvent?????????????????????sql
                // ???????????????dmlSqlMap??????????????????????????????????????????dml rowData?????????????????????sql
                lastEntryMap.put(i,entry);

                // ?????????????????????RowsQueryLogEvent???????????????????????????rowData?????????????????????????????????????????????
                // ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            }

            if(CollectionUtils.isNotEmpty(singleEntryResult)){
                // dml?????????????????????sql
                if(!rowChange.getIsDdl()) {
                    dmlRelateOriginalSql(i, entry, lastEntryMap, singleEntryResult);
                }

                totalEntryResult.addAll(singleEntryResult);
            }
        }

        if (totalEntryResult.size() > 0) {
            logDelay(entrys.get(0));
        }

        return totalEntryResult;
    }

    /**
     * dml entry?????????????????????sql
     * */
    private void dmlRelateOriginalSql(int index, CanalEntry.Entry entry,
                                      Map<Integer,CanalEntry.Entry> lastEntryMap, List<Binlog2JsonModel> singleEntryResult){
        int preEntryIndex = index-1;
        CanalEntry.Entry preEntry;
        // ???ddl??????dml???????????????????????????entry????????????sql
        if(index>0 && (preEntry = lastEntryMap.get(preEntryIndex)) != null){
            // ??????RowsQueryLogEvent?????????canal?????????????????????????????????????????????????????????????????????entry?????????????????????
            // ?????????anal.instance.filter.druid.ddl=true, RowsQueryLogEvent???entry??????tableName
            if(StringUtils.equals(entry.getHeader().getTableName(),preEntry.getHeader().getTableName())){
                // ??????????????????entry?????????sql?????????

                CanalEntry.RowChange preRowChange = DrcCanalEntryParseUtil.parseRowChange(preEntry);
                String originalSql = preRowChange.getSql();
                // DML????????????binlogModel??????????????????sql
                singleEntryResult.forEach(
                        item->item.setOriginalSql(originalSql)
                );
            }
        }
    }

    private List<Binlog2JsonModel> handleCanalEntry(CanalEntry.Entry entry,CanalEntry.RowChange rowChange){
        if(rowChange.getIsDdl()){
            // DDL??????
            if(supportDDLSync){
                // ??????ddl?????????ddl?????????model
                String ddlSql = rowChange.getSql();
                Binlog2JsonModel bm = new Binlog2JsonModel();
                bm.setAction(Binlog2JsonModel.ACTION_DDL);
                bm.setDbName(entry.getHeader().getSchemaName());
                bm.setTableName(entry.getHeader().getTableName());
                bm.setOriginalSql(ddlSql);
                return Collections.singletonList(bm);
            }else{
                // ?????????ddl???????????????
                return null;
            }
        }else{
            // ???DDL???????????????DML
            if(supportDDLSync && DDLSyncFilterDML){
                // ????????????DDL,???????????????DML?????????????????????
                return null;
            }else{
                // DML????????????

                // ??????eventType
                CanalEntry.EventType eventType = rowChange.getEventType();
                // ?????????????????????RowsQueryLogEvent??????QUERY????????????
                if (eventType == CanalEntry.EventType.QUERY) {
                    return null;
                }

                // ?????????????????????????????????
                if (CollectionUtils.isEmpty(rowChange.getRowDatasList())) {
                    return null;
                }

                List<Binlog2JsonModel> binlog2JsonModelList = new ArrayList<>();
                for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                    Binlog2JsonModel bm = new Binlog2JsonModel();
                    bm.setDbName(entry.getHeader().getSchemaName());
                    bm.setTableName(entry.getHeader().getTableName());
                    bm.setOriginalSql(rowChange.getSql());

                    if (eventType == CanalEntry.EventType.DELETE) {
                        bm.setAction(Binlog2JsonModel.ACTION_DELETE);
                        handleBefore(bm, rowData);
                        binlog2JsonModelList.add(bm);
                    } else if (eventType == CanalEntry.EventType.INSERT) {
                        bm.setAction(Binlog2JsonModel.ACTION_INSERT);
                        handleAfter(bm, rowData);
                        binlog2JsonModelList.add(bm);
                    } else {
                        bm.setAction(Binlog2JsonModel.ACTION_UPDATE);
                        handleBefore(bm, rowData);
                        handleAfter(bm, rowData);
                        binlog2JsonModelList.add(bm);
                    }
                    String gtid = entry.getHeader().getGtid();
                    bm.setGtId(gtid);
                }

                return binlog2JsonModelList;
            }
        }
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
            if (!column.getIsNull()) {
                map.put(column.getName(), column.getValue());
            }
        }
        columnsKey.setColumns(map);
        return columnsKey;
    }

    private void logDelay(CanalEntry.Entry entry) {
        long executeTime = entry.getHeader().getExecuteTime();
        long delay = TimeFactory.currentTimeMillis() - executeTime;
        if (delay < 0) {
            return;
        }
        delayTotal += delay;
        if (Math.abs(delayCounter++) % 10 == 0) {
            if (GlobalConfigUtil.printBinlogDelayTime()) {
                // ???10???????????????.
                String tip = String.format("binlog ????????????, %s ??????, TableName=%s", delayTotal / 10, entry.getHeader().getTableName());
                log.info(tip);
                delayTotal = 0;
            }
        }
    }

    @Data
    public static class ColumnsKey {
        Map<String, Object> columns;
        String key;
    }


}
