package com.timevale.drc.base.mysql;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * @author gwk_2
 * @date 2021/4/22 11:39
 */
@Slf4j
public class GetTableSQLRowMapper implements RowMapper<String> {

    @Override
    public String mapRow(ResultSet rs, int rowNum) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int columnCount = md.getColumnCount();
        if (columnCount > 0) {
            log.info("table SQL = {}", rs.getString(2));
            return rs.getString(2);
        }
        return null;
    }
}
