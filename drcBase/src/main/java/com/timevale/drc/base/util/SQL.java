package com.timevale.drc.base.util;

import com.timevale.drc.base.mysql.MySqlKeyUtil;

/**
 * @author gwk_2
 * @date 2021/3/5 16:15
 */
public class SQL {

    /**
     * 获取表名, 通常是验证表是否存在.
     */
    public static String getTable(String database, String tableName) {
        // 请查看示例 https://gist.github.com/retanoj/5fd369524a18ab68a4fe7ac5e0d121e8
        // select `TABLE_NAME` from `INFORMATION_SCHEMA`.`TABLES` where `TABLE_SCHEMA`='crawler' and `TABLE_NAME`='sand_hex_test'
        return String.format("SELECT table_name FROM information_schema.TABLES WHERE `TABLE_SCHEMA`='%s' and table_name ='%s'", database, tableName);
    }

    /**
     * 获取该 table 的字段元数据.
     */
    public static String getAllFieldMetaData(String table) {
        if (MySqlKeyUtil.exists(table)) {
            table = MySqlKeyUtil.conv(table);
        }
        return "show full columns from " + table;
    }

    /**
     * 当主键是 int 类型时, 获取 range sql 语句.
     *
     * @param symbol > , >=
     */
    public static String getIntPkList(String pkName, String table, Object minPk, Object maxPk, Integer limit, String symbol) {
        // select id from table where id >or>= min and id < max order by id limit 10
        String sql = "select %s from %s where %s %s %s and %s <= %s order by %s limit %s";
        // ? SQL 注入???? 不要紧的.
        return String.format(sql, pkName, table, pkName, symbol, minPk, pkName, maxPk, pkName, limit);
    }

    public static String getIntPkList(String pkName, String table, Object minPk, Object maxPk, Integer limit, String symbol, String whereStatement) {
        // select id from table where id >or>= min and id < max order by id limit 10
        String sql = "select %s from %s %s and %s %s %s and %s <= %s order by %s limit %s";
        // ? SQL 注入???? 不要紧的.
        return String.format(sql, pkName, table, whereStatement, pkName, symbol, minPk, pkName, maxPk, pkName, limit);
    }


    /**
     * 当主键是  varchar 类型时, 获取 range sql 语句(需要加上 '' 双引号).
     */
    public static String getStringPkList(String pkName, String table, Object minPk, Object maxPk, Integer limit, String symbol) {
        // select id from table where id >or>= min and id < max order by id limit 10
        String sql = "select %s from %s where %s %s '%s' and %s <= '%s' order by %s limit %s";
        // ? SQL 注入???? 不要紧的.
        return String.format(sql, pkName, table, pkName, symbol, minPk, pkName, maxPk, pkName, limit);
    }

    /**
     * 当主键是  varchar 类型时, 获取 range sql 语句(需要加上 '' 双引号).
     */
    public static String getStringPkList(String pkName, String table, Object minPk, Object maxPk, Integer limit, String symbol, String whereStatement) {
        // select id from table where id >or>= min and id < max order by id limit 10
        String sql = "select %s from %s %s and %s %s '%s' and %s <= '%s' order by %s limit %s";
        // ? SQL 注入???? 不要紧的.
        return String.format(sql, pkName, table, pkName, whereStatement, symbol, minPk, pkName, maxPk, pkName, limit);
    }

    /**
     * 执行 select 1 ,看看连接是否正常.
     */
    public static String select1() {
        return "select 1";
    }

    public static String getDeleteSQL(String tableName, String idName) {
        return "delete from " + tableName + " where " + idName + " = ?";
    }

    public static String getSimpleSQL(String pkName, String table, String whereStatement) {
        String sql = "select %s from %s %s";
        return String.format(sql, pkName, table, whereStatement);
    }
}
