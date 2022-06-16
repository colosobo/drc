package com.timevale.drc.worker.service.sink.mysql;

import com.timevale.drc.base.binlog.Binlog2JsonModel;
import com.timevale.drc.base.sinkConfig.MySQLSinkConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

/**
 * @author gwk_2
 * @date 2021/4/20 23:13
 * <p>
 * 大部分情况下,只针对全量的  MySQL 同步. 增量不需要批量.
 * 所以,没有针对 delete 和 update 做任何操作.
 */
@Slf4j
public class MySqlBatchSink extends MySqlSink {

    public MySqlBatchSink(JdbcTemplate jdbcTemplate, MySQLSinkConfig mysqlSinkConfig) {
        super(jdbcTemplate, mysqlSinkConfig);
    }

    @Override
    public void sink(List<Binlog2JsonModel> data) {
        if (data.size() <= 0) {
            return;
        }
        String tableName = super.getCustomTableName();
        if (StringUtils.isBlank(tableName)) {
            // 有可能 list 里面是不同的表吗? 目前的设计上, 讲道理不可能.
            tableName = data.get(0).getTableName();
        }

        // 如果一条一条过来的,通常就是 binlog.
        if (data.size() == 1) {
            super.sink(data);
            return;
        }

        tryBuildColumns(tableName);

        batchInsert(data, tableName);
    }

    private void batchInsert(List<Binlog2JsonModel> list, String tableName) {
        PreparedStatement preparedStatement = null;
        Connection connection = null;
        try {
            connection = jdbcTemplate.getDataSource().getConnection();
            final String sql = insertSqlMap.get(tableName);
            preparedStatement = connection.prepareStatement(sql);

            for (Binlog2JsonModel model : list) {
                int index = 1;
                for (String column : super.columnsMap.get(tableName)) {
                    index = setObject(model.getAfter().get(column), preparedStatement, index, column, tableName);
                }
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        } catch (SQLIntegrityConstraintViolationException e) {
            // ignore 主键冲突, 直接忽略
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                    connection.close();
                } catch (SQLException e) {
                    log.warn(e.getSQLState(), e);
                }
            }
        }
    }
}
