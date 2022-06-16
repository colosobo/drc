package com.timevale.drc.worker.service.sink.mysql;

import com.timevale.drc.base.Sink;
import com.timevale.drc.base.alarm.AlarmUtil;
import com.timevale.drc.base.binlog.Binlog2JsonModel;
import com.timevale.drc.base.mysql.MySqlKeyUtil;
import com.timevale.drc.base.sinkConfig.MySQLSinkConfig;
import com.timevale.drc.base.util.DateUtils;
import com.timevale.drc.base.util.JdbcTemplateManager;
import com.timevale.drc.base.util.SQL;
import com.timevale.drc.worker.service.sink.util.MysqlSinkUtil;
import com.timevale.drc.worker.service.sink.util.SqlParseUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.timevale.drc.base.binlog.Binlog2JsonModel.*;

/**
 * @author gwk_2
 */
@Slf4j
@Data
public class MySqlSink implements Sink<Binlog2JsonModel> {

    protected final JdbcTemplate jdbcTemplate;
    protected Map<String/*tableName*/, List<String/* column */>> columnsMap = new ConcurrentHashMap<>();
    protected final Map<String/*tableName*/, Map<String/* column name*/, JdbcTemplateManager.FieldType>> typeMapMap = new HashMap<>();
    protected final Map<String, String> insertSqlMap = new HashMap<>();

    /**
     * todo 支持回环同步.
     */
    protected String sinkUrl;
    /** 有可能是自定义的表名. */
    protected String customTableName;

    protected MySQLSinkConfig mysqlSinkconfig;

    private final JdbcTemplateManager jdbcTemplateManager = new JdbcTemplateManager();

    public MySqlSink(JdbcTemplate jdbcTemplate, MySQLSinkConfig mysqlSinkconfig) {
        this.mysqlSinkconfig = mysqlSinkconfig;
        this.jdbcTemplate = jdbcTemplate;
        this.customTableName = mysqlSinkconfig.tableName;
    }

    @Override
    public void start() {
        log.info("{} MySqlSink start.", this.customTableName);
    }

    @Override
    public void stop() {
        try {
            jdbcTemplate.getDataSource().getConnection().close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void sink(List<Binlog2JsonModel> list) {
        for (Binlog2JsonModel model : list) {
            if(this.mysqlSinkconfig.supportLoopSync && MysqlLoopSyncProcessUtil.isFromDRCSql(model.getOriginalSql())){
                log.info("drc回环sql过滤 {}",model.getOriginalSql());
                // 带有drc标识的sql不进行同步（只同步真实业务产生的sql）
                continue;
            }

            String tableName = this.customTableName;
            if (StringUtils.isBlank(this.customTableName)) {
                tableName = model.getTableName();
            }

            String action = model.getAction();
            if (action.equalsIgnoreCase(ACTION_DDL)) {
                executeDDLSql(model, mysqlSinkconfig);
                // 执行ddl语句后，表元数据可能有改动，刷新cache
                refreshMetaInfoCache(tableName);
                continue;
            }

            // dml构造sql列信息
            tryBuildColumns(tableName);
            if (action.equalsIgnoreCase(ACTION_INSERT)) {
                executeInsertSQL(model, tableName);
                continue;
            }
            if (action.equalsIgnoreCase(ACTION_UPDATE)) {
                executeUpdateSQL(model, tableName);
                continue;
            }
            if (action.equalsIgnoreCase(ACTION_DELETE)) {
                executeDeletedSQL(model, tableName);
            }
        }
    }

    protected void executeDDLSql(Binlog2JsonModel model, MySQLSinkConfig mysqlSinkconfig) {
        String ddlSql = model.getOriginalSql();

        log.info("executeDDLSql originalSql=" + ddlSql);

        // 如果有自定义表名, 需要替换.
        if (StringUtils.isNotBlank(this.customTableName)) {
            ddlSql = SqlParseUtil.replaceMysqlDDLTableName(ddlSql, this.customTableName);
            log.info("executeDDLSql afterReplaceMysqlDDLTableNameSql=" + ddlSql);
        }

        // 库名删除
        ddlSql = SqlParseUtil.removeMysqlDDLDatabaseName(ddlSql, this.mysqlSinkconfig.database);
        log.info("executeDDLSql afterReplaceMysqlDDLDatabaseNameSql=" + ddlSql);

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = jdbcTemplate.getDataSource().getConnection();
            if(mysqlSinkconfig.supportLoopSync) {
                ddlSql = MysqlLoopSyncProcessUtil.addDRCUniqueFlag(ddlSql);
            }
            log.info("executeDDLSql addDRCFlagSql=" + ddlSql);
            preparedStatement = connection.prepareStatement(ddlSql);
            preparedStatement.setQueryTimeout(MysqlSinkUtil.getDDLSyncTimeout(mysqlSinkconfig.getTableName()));
            preparedStatement.execute();
            log.info("executeDDLSql success");
        } catch (BadSqlGrammarException badSqlGrammarException) {
            log.error("ddl error，maybe repeated execute ddl", badSqlGrammarException);
        } catch (Exception e) {
            log.warn("executeDDLSql error:" + e.getMessage(), e);
            AlarmUtil.pushAlarm2Admin("MySQL Sink 执行 ddl 失败, ddlSql = " + ddlSql);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                    connection.close();
                } catch (SQLException ex) {
                    log.warn(ex.getSQLState(), ex);
                }
            }
        }
    }

    protected void executeDeletedSQL(Binlog2JsonModel model, String tableName) {
        Object rowKeyValue = model.getBefore().get(model.getRowKey());

        String sql = SQL.getDeleteSQL(tableName, model.getRowKey());
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = jdbcTemplate.getDataSource().getConnection();
            if(mysqlSinkconfig.supportLoopSync) {
                sql = MysqlLoopSyncProcessUtil.addDRCUniqueFlag(sql);
            }
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setObject(1, rowKeyValue);
            log.info("delete sql=" + preparedStatement.toString());
            preparedStatement.execute();
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            AlarmUtil.pushAlarm2Admin("MySQL Sink 执行 delete 失败, sql = " + sql);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                    connection.close();
                } catch (SQLException ex) {
                    log.warn(ex.getSQLState(), ex);
                }
            }
        }
    }

    protected void executeUpdateSQL(Binlog2JsonModel model, String tableName) {
        StringBuilder sql = new StringBuilder("update " + tableName + "  set ");
        String rowKey = MySqlKeyUtil.processKeywordColumn(model.getRowKey());

        for (String column : model.getAfter().keySet()) {
            String finalColumnName = MySqlKeyUtil.processKeywordColumn(column);
            sql.append(finalColumnName).append(" = ").append("?").append(",");
        }

        sql = new StringBuilder(sql.substring(0, sql.length() - 1));

        sql.append(" where ").append(rowKey).append(" = ").append("?");
        PreparedStatement preparedStatement = null;
        Connection connection = null;
        try {
            connection = jdbcTemplate.getDataSource().getConnection();
            if(mysqlSinkconfig.supportLoopSync) {
                MysqlLoopSyncProcessUtil.addDRCUniqueFlag(sql);
            }
            preparedStatement = connection.prepareStatement(sql.toString());
            int index = 1;
            for (String key : model.getAfter().keySet()) {
                index = setObject(model.getAfter().get(key), preparedStatement, index, key, tableName);
            }
            preparedStatement.setObject(index, model.getAfter().get(rowKey));
            log.info("update sql=" + preparedStatement.toString());
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            log.error("tableName = {}, msg={}", tableName, e.getMessage());
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                    connection.close();
                } catch (SQLException ex) {
                    log.warn(ex.getSQLState(), ex);
                }
            }
        }
    }

    protected int setObject(Object model, PreparedStatement preparedStatement, int index,
                            String key, String tableName) throws SQLException {

        if (typeMapMap.get(tableName) != null) {
            if (typeMapMap.get(tableName).get(key) == JdbcTemplateManager.FieldType.YEAR && model instanceof Date) {
                // year 类型, 可能会导致问题, 例如出现 2011-01-01, 此时, 如果目标库的字段长度是 4 , 会导致无法插入目标数据库.
                preparedStatement.setObject(index++, DateUtils.format((Date) model, "YYYY"));
            } else {
                preparedStatement.setObject(index++, model);
            }
        }
        return index;
    }

    private void executeInsertSQL(Binlog2JsonModel model, String tableName) {
        List<String> columns = columnsMap.get(tableName);

        Map<String, Object> after = model.getAfter();

        PreparedStatement preparedStatement = null;
        Connection connection = null;
        try {
            connection = jdbcTemplate.getDataSource().getConnection();
            String sql = insertSqlMap.get(tableName);
            if(mysqlSinkconfig.supportLoopSync) {
                sql = MysqlLoopSyncProcessUtil.addDRCUniqueFlag(sql);
            }
            preparedStatement = connection.prepareStatement(sql);
            int index = 1;
            for (String column : columns) {
                index = setObject(after.get(column), preparedStatement, index, column, tableName);
            }
            log.info("insert sql=" + preparedStatement.toString());
            preparedStatement.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            // ignore 主键冲突, 直接忽略
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                    connection.close();
                } catch (SQLException ex) {
                    log.warn(ex.getSQLState(), ex);
                }
            }
        }
    }

    /**
     * 执行ddl语句后，表元数据可能有改动，刷新cache
     * */
    private void refreshMetaInfoCache(String tableName){
        this.columnsMap.remove(tableName);
        // drop table时tryBuildColumns会有问题，待优化
//        tryBuildColumns(tableName);
    }

    protected void tryBuildColumns(String tableName) {
        List<String> columns;
        if ((columnsMap.get(tableName)) == null) {
            String getAllFieldMetaDataSql = SQL.getAllFieldMetaData(tableName);
            // 字段列.
            columns = jdbcTemplate.queryForObject(getAllFieldMetaDataSql, (rs, rowNum) -> {
                List<String> set = new ArrayList<>();
                set.add(rs.getString(1));
                while (rs.next()) {
                    String string = rs.getString(1);
                    set.add(string);
                }
                return set;
            });

            Map<String, JdbcTemplateManager.FieldType> typeMap = new HashMap<>();

            for (String column : columns) {
                JdbcTemplateManager.FieldType fieldType = jdbcTemplateManager.getFieldType(jdbcTemplate, tableName, column);
                typeMap.put(column, fieldType);
            }

            if (MySqlKeyUtil.exists(tableName)) {
                tableName = MySqlKeyUtil.conv(tableName);
            }

            StringBuilder sb = new StringBuilder(String.format("INSERT IGNORE INTO %s (", tableName));
            for (String column : columns) {
                // 关键字, 需要加反引号.
                column = MySqlKeyUtil.processKeywordColumn(column);

                sb.append(column).append(",");
            }

            sb = new StringBuilder(sb.substring(0, sb.length() - 1));

            sb.append(") values (");

            StringBuilder finalSql = sb;
            columns.forEach(i -> finalSql.append("?").append(","));

            sb = new StringBuilder(sb.substring(0, sb.length() - 1));
            sb.append(")");

            insertSqlMap.put(tableName, sb.toString());
            columnsMap.put(tableName, columns);
            typeMapMap.put(tableName, typeMap);
        }
    }
}
