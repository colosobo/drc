package com.timevale.drc.pd.service.full.resolve;

import com.timevale.drc.base.util.JdbcTemplateManager;
import com.timevale.drc.base.util.SQL;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author gwk_2
 * @date 2021/3/3 11:37
 */
@Slf4j
public class TableWithWhereSplitResolver extends TableSplitResolver {


    public TableWithWhereSplitResolver(LinkedBlockingQueue<Slice> channel) {
        super(channel);
    }

    <T> String getSQL(String tableName, int limit, String whereStatement, String pkName, JdbcTemplateManager.FieldType fieldType, T maxPk, T tmpMinPk, String symbol) {
        String sql = null;
        if (fieldType.findClass().equals(Integer.class)) {
            sql = SQL.getIntPkList(pkName, tableName, tmpMinPk, maxPk, limit, symbol, whereStatement);
        }
        if (fieldType.findClass().equals(String.class)) {
            sql = SQL.getStringPkList(pkName, tableName, tmpMinPk, maxPk, limit, symbol, whereStatement);
        }
        return sql;
    }

    Map<String, String> getMinMax(String pkName, String tableName, String whereStatement) {
        String minSql = String.format("select min(%s) from %s %s", pkName, tableName, whereStatement);
        String maxSql = String.format("select max(%s) from %s %s", pkName, tableName, whereStatement);

        Map<String, String> map = new HashMap<>();
        map.put(minSql, maxSql);

        return map;
    }

}
