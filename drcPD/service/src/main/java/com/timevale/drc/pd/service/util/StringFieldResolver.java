package com.timevale.drc.pd.service.util;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * @author gwk_2
 * @date 2021/3/23 19:10
 */
public class StringFieldResolver implements RowMapper<String> {
    @Override
    public String mapRow(ResultSet rs, int rowNum) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();//获取键名
        int columnCount = md.getColumnCount();//获取行的数量
        // 可能有 2个主键,但是只取第一个
        if (columnCount > 0) {
            return rs.getObject(1).toString();
        }
        throw new RuntimeException("没有主键....");
    }

}
