package com.timevale.drc.pd.service.util;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * @author gwk_2
 * @date 2021/3/23 19:10
 */
public class IntFieldResolver implements RowMapper<Integer> {
    @Override
    public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();//获取键名
        int columnCount = md.getColumnCount();//获取行的数量
        // 可能有 2个主键,但是只取第一个
        if (columnCount > 0) {
            return rs.getInt(1);
        }
        throw new RuntimeException("没有主键....");
    }

}
