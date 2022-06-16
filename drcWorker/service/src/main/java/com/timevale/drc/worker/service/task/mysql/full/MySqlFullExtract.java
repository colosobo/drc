package com.timevale.drc.worker.service.task.mysql.full;

import com.google.common.collect.Lists;
import com.timevale.drc.base.Extract;
import com.timevale.drc.base.TaskStateEnum;
import com.timevale.drc.base.binlog.Binlog2JsonModel;
import com.timevale.drc.base.dao.DrcDbConfigMapper;
import com.timevale.drc.base.dao.DrcSubTaskFullConfigMapper;
import com.timevale.drc.base.log.TaskLog;
import com.timevale.drc.base.model.DrcDbConfig;
import com.timevale.drc.base.model.DrcSubTaskFullConfig;
import com.timevale.drc.base.model.DrcSubTaskFullSliceDetail;
import com.timevale.drc.base.util.JdbcTemplateManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 提取 mysql 全量数据.
 *
 * @author gwk_2
 * @date 2021/3/8 12:48
 */
@Slf4j
public class MySqlFullExtract implements Extract<List<Binlog2JsonModel>> {

    private final DrcSubTaskFullConfigMapper fullConfigMapper;
    private final DrcDbConfigMapper drcDbConfigMapper;
    private final JdbcTemplateManager jdbcTemplateManager;

    private final DrcSubTaskFullSliceDetail drcSubTaskFullSliceDetail;
    private DrcSubTaskFullConfig drcSubTaskFullConfig;
    private DrcDbConfig drcDbConfig;
    private final int limit;
    private JdbcTemplateManager.FieldType fieldType;
    private JdbcTemplate jdbcTemplate;
    private String selectFieldList;
    private boolean firstSelect = true;
    private TaskLog taskLog;

    public MySqlFullExtract(DrcSubTaskFullSliceDetail drcSubTaskFullSliceDetail,
                            DrcSubTaskFullConfigMapper fullConfigMapper,
                            DrcDbConfigMapper drcDbConfigMapper,
                            JdbcTemplateManager jdbcTemplateManager,
                            int limit, TaskLog taskLog) {

        this.drcSubTaskFullSliceDetail = drcSubTaskFullSliceDetail;
        this.fullConfigMapper = fullConfigMapper;
        this.drcDbConfigMapper = drcDbConfigMapper;
        this.jdbcTemplateManager = jdbcTemplateManager;
        this.limit = limit;
        this.taskLog = taskLog;
    }

    @Override
    public List<Binlog2JsonModel> extract() {
        Integer fullConfigId = drcSubTaskFullSliceDetail.getDrcSubTaskFullConfigId();
        if (drcSubTaskFullSliceDetail.getState() == TaskStateEnum.OVER.code) {
            return Lists.newArrayList();
        }

        if (drcSubTaskFullConfig == null) {
            drcSubTaskFullConfig = fullConfigMapper.selectByPrimaryKey(fullConfigId);
        }

        String pkName = drcSubTaskFullSliceDetail.getSlicePkName();
        String sliceMinPk = drcSubTaskFullSliceDetail.getSliceCursor();
        String sliceMaxPk = drcSubTaskFullSliceDetail.getSliceMaxPk();
        boolean isLast = drcSubTaskFullSliceDetail.getIsLastSlice().equals(DrcSubTaskFullSliceDetail.IS_LAST_SLICE_TRUE);

        String tableName = drcSubTaskFullConfig.getTableName();

        Integer dbConfigId = drcSubTaskFullConfig.getDbConfigId();

        if (drcDbConfig == null) {
            drcDbConfig = drcDbConfigMapper.selectByPrimaryKey(dbConfigId);
        }

        if (drcSubTaskFullSliceDetail.getSliceCursor().equals(drcSubTaskFullSliceDetail.getSliceMaxPk())) {
            if (!firstSelect) {
                if (taskLog != null) {
                    taskLog.info("同步结束.......");
                }
                return Lists.newArrayList();
            }
        }

        firstSelect = false;

        String url = drcDbConfig.getUrl();
        String username = drcDbConfig.getUsername();
        String password = drcDbConfig.getPassword();
        String databaseName = drcDbConfig.getDatabaseName();

        if (jdbcTemplate == null) {
            jdbcTemplate = jdbcTemplateManager.get(url, username, password, databaseName);
        }
        if (selectFieldList == null) {
            selectFieldList = "*";
        }

        if (fieldType == null) {
            fieldType = jdbcTemplateManager.getFieldType(jdbcTemplate, tableName, pkName);
        }
        String whereStatement = drcSubTaskFullConfig.getWhereStatement();
        whereStatement = whereStatement == null ? "" : whereStatement.replace("where", " and ");
        String sql;
        SqlFormat sqlFormat = SqlFormat.SqlFormatFactory.create(fieldType, selectFieldList, tableName, pkName, sliceMinPk, sliceMaxPk, limit, isLast, whereStatement);
        // 如果是第一条. 使用 >=, 防止漏掉.
        if (drcSubTaskFullSliceDetail.getSliceMinPk().equals(drcSubTaskFullSliceDetail.getSliceCursor())) {
            sql = sqlFormat.select(">=");
        } else {
            sql = sqlFormat.select(">");
        }

        List<Map<String, Object>> result = jdbcTemplate.query(sql, new ListRowMapper());
        if (result.size() == 0) {
            return Lists.newArrayList();
        }
        if (result.size() == 1 && limit != 1) {
            drcSubTaskFullSliceDetail.setState(TaskStateEnum.OVER.code);
        }

        return conv(pkName, tableName, result);
    }

    @Override
    public void ack() {

    }

    private List<Binlog2JsonModel> conv(String pkName, String tableName, List<Map<String, Object>> result) {
        List<Binlog2JsonModel> re = new ArrayList<>();

        for (Map<String, Object> objectMap : result) {
            Binlog2JsonModel model = new Binlog2JsonModel();
            model.setRowKey(pkName);
            model.setAfter(objectMap);
            model.setTableName(tableName);
            model.setAction(Binlog2JsonModel.ACTION_INSERT);
            model.setDbName(drcDbConfig.getDatabaseName());
            re.add(model);
        }
        return re;
    }

    static class ListRowMapper implements RowMapper<Map<String, Object>> {

        @Override
        public Map<String, Object> mapRow(ResultSet result, int rowNum) throws SQLException {
            Map<String, Object> map = new HashMap<>();
            ResultSetMetaData metaData = result.getMetaData();  //获取列集
            int columnCount = metaData.getColumnCount(); //获取列的数量
            for (int i = 0; i < columnCount; i++) { //循环列
                String columnName = metaData.getColumnName(i + 1); //通过序号获取列名,起始值为1
                Object columnValue = result.getObject(columnName);  //通过列名获取值.如果列值为空,columnValue为null,不是字符型
                map.put(columnName, columnValue);
            }
            return map;
        }
    }
}
