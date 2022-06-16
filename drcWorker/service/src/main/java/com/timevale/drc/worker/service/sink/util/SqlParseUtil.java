package com.timevale.drc.worker.service.sink.util;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.timevale.drc.worker.service.sink.util.visitor.RemoveDatabaseNameVisitor;
import com.timevale.drc.worker.service.sink.util.visitor.ReplaceTableNameVisitor;

public class SqlParseUtil {

    /**
     * 替换mysql ddl语句中的表名
     * */
    public static String replaceMysqlDDLTableName(String originalSql, String needReplaceTableName){
        MySqlStatementParser parser = new MySqlStatementParser(originalSql);
        SQLStatement statement = parser.parseStatement();
        statement.accept(new ReplaceTableNameVisitor(needReplaceTableName));
        return SQLUtils.toMySqlString(statement);
    }

    /**
     * 替换mysql ddl语句中的库名
     * */
    public static String removeMysqlDDLDatabaseName(String originalSql, String needReplaceDatabaseName){
        MySqlStatementParser parser = new MySqlStatementParser(originalSql);
        SQLStatement statement = parser.parseStatement();
        statement.accept(new RemoveDatabaseNameVisitor());
        return SQLUtils.toMySqlString(statement);
    }
}
