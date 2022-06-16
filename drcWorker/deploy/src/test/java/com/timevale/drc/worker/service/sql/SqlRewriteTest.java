package com.timevale.drc.worker.service.sql;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import com.timevale.drc.worker.service.sink.util.visitor.RemoveDatabaseNameVisitor;
import org.junit.Assert;
import org.junit.Test;

public class SqlRewriteTest {

    @Test
    public void testSqlDatabaseRewrite(){
        String originalSql = "/* ApplicationName=DataGrip 2022.1 */ " +
                "ALTER TABLE dbdc_test.yunge_test MODIFY licenseNumber VARCHAR(242)";

        MySqlStatementParser parser = new MySqlStatementParser(originalSql);
        SQLStatement statement = parser.parseStatement();
        statement.accept(new RemoveDatabaseNameVisitor());

        String sqlAfterRewrite = SQLUtils.toMySqlString(statement);
        System.out.println(sqlAfterRewrite);

        MySqlStatementParser parser2 = new MySqlStatementParser(sqlAfterRewrite);
        SQLStatement statement2 = parser2.parseStatement();

        statement2.accept(new MySqlASTVisitorAdapter(){
            @Override
            public boolean visit(SQLExprTableSource x) {
                Assert.assertNotEquals(x.getExpr().toString(),"dbdc_test.yunge_test");
                Assert.assertEquals(x.getExpr().toString(),"yunge_test");
                return true;
            }
        });
    }
}
