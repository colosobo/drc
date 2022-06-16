package com.timevale.drc.worker.service.sink.util.visitor;

import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;

/**
 * @author shanreng
 * 表名替换
 */
public class ReplaceTableNameVisitor extends MySqlASTVisitorAdapter {

    private String replaceTableName;

    public ReplaceTableNameVisitor(String replaceTableName) {
        this.replaceTableName = replaceTableName;
    }

    @Override
    public boolean visit(SQLExprTableSource x) {
        x.setExpr(replaceTableName);
        return true;
    }
}
