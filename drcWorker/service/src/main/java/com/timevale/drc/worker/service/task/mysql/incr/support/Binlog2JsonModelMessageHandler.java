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

        // key为遍历的下标i，value为对应的entry
        Map<Integer,CanalEntry.Entry> lastEntryMap = new HashMap<>();

        for (int i = 0; i < entrys.size(); i++) {
            CanalEntry.Entry entry = entrys.get(i);

            // 事物，心跳直接跳过
            if (excludeSet.contains(entry.getEntryType())) {
                continue;
            }

            // 解析rowChange
            CanalEntry.RowChange rowChange = DrcCanalEntryParseUtil.parseRowChange(entry);

            List<Binlog2JsonModel> singleEntryResult = handleCanalEntry(entry,rowChange);

            if(DrcCanalEntryParseUtil.isRowsQueryLogEventEntry(rowChange)){
                // RowsQueryLogEvent中包含了原始的sql
                // 将其保存在dmlSqlMap中，下一个事件可能就是对应的dml rowData事件关联其原始sql
                lastEntryMap.put(i,entry);

                // 注意：队列满时RowsQueryLogEvent事件可能会和其后的rowData事件分离，导致无法过滤回环同步
                // 但这种情况发生的概率很低，后续的回环同步过程中不可能每次都被截断，回环同步最终会收敛
            }

            if(CollectionUtils.isNotEmpty(singleEntryResult)){
                // dml尝试着关联原始sql
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
     * dml entry尝试着关联原始sql
     * */
    private void dmlRelateOriginalSql(int index, CanalEntry.Entry entry,
                                      Map<Integer,CanalEntry.Entry> lastEntryMap, List<Binlog2JsonModel> singleEntryResult){
        int preEntryIndex = index-1;
        CanalEntry.Entry preEntry;
        // 非ddl，即dml才尝试着通过上一个entry获取原始sql
        if(index>0 && (preEntry = lastEntryMap.get(preEntryIndex)) != null){
            // 由于RowsQueryLogEvent不会被canal的白名单表达式过滤，需要通过表名判断相邻的两个entry是否是同一张表
            // 注意：anal.instance.filter.druid.ddl=true, RowsQueryLogEvent的entry才有tableName
            if(StringUtils.equals(entry.getHeader().getTableName(),preEntry.getHeader().getTableName())){
                // 是同一张表的entry，进行sql的关联

                CanalEntry.RowChange preRowChange = DrcCanalEntryParseUtil.parseRowChange(preEntry);
                String originalSql = preRowChange.getSql();
                // DML的每一个binlogModel都设置上原始sql
                singleEntryResult.forEach(
                        item->item.setOriginalSql(originalSql)
                );
            }
        }
    }

    private List<Binlog2JsonModel> handleCanalEntry(CanalEntry.Entry entry,CanalEntry.RowChange rowChange){
        if(rowChange.getIsDdl()){
            // DDL事件
            if(supportDDLSync){
                // 支持ddl，构造ddl类型的model
                String ddlSql = rowChange.getSql();
                Binlog2JsonModel bm = new Binlog2JsonModel();
                bm.setAction(Binlog2JsonModel.ACTION_DDL);
                bm.setDbName(entry.getHeader().getSchemaName());
                bm.setTableName(entry.getHeader().getTableName());
                bm.setOriginalSql(ddlSql);
                return Collections.singletonList(bm);
            }else{
                // 不支持ddl，直接过滤
                return null;
            }
        }else{
            // 非DDL，默认就是DML
            if(supportDDLSync && DDLSyncFilterDML){
                // 支持同步DDL,且需要过滤DML直接返回过滤掉
                return null;
            }else{
                // DML解析开始

                // 解析eventType
                CanalEntry.EventType eventType = rowChange.getEventType();
                // 查询直接跳过（RowsQueryLogEvent也是QUERY类型的）
                if (eventType == CanalEntry.EventType.QUERY) {
                    return null;
                }

                // 变更数据为空，直接跳过
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
                // 取10次的平均值.
                String tip = String.format("binlog 延迟时间, %s 毫秒, TableName=%s", delayTotal / 10, entry.getHeader().getTableName());
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
