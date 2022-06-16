package com.timevale.drc.worker.service.sink.util.visitor;

import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;

/**
 * @author shanreng
 * 移除库名
 * 举个例子：源库名为e_test,全库级别同步表达式为*,则canal的AviaterRegexFilter过滤表达式为e_test.*$
 * 目标库名为e_test_copy,则目标库的binlog e_test_copy也是符合e_test.*$的，目前没有解决回环同步问题，所以会造成无限循环的自同步
 *
 * 具体可以看最下面被注释的main方法
 */
public class RemoveDatabaseNameVisitor extends MySqlASTVisitorAdapter {

    /**
     * 包含库名的表达式元素长度
     * */
    private final int hasDatabaseExpElementsSize = 2;

    public RemoveDatabaseNameVisitor() {
    }

    @Override
    public boolean visit(SQLExprTableSource x) {
        String expr = x.getExpr().toString();
        String[] splitDataBaseAndTable = expr.split("\\.");
        if(splitDataBaseAndTable.length == hasDatabaseExpElementsSize){
            // 将库名去掉，表名保留
            x.setExpr(splitDataBaseAndTable[1]);
        }

        return true;
    }

//    public static void main(String[] args) {
//        AviaterRegexFilter aviaterRegexFilter = new AviaterRegexFilter("dbdc_test.*$");
//        System.out.println(aviaterRegexFilter.filter("dbdc_test_copy.yunge_test")); // true
//        System.out.println(aviaterRegexFilter.filter("copy_dbdc_test.yunge_test")); // false
//
//        System.out.println(aviaterRegexFilter.filter("abc.yunge_test")); // false
//    }
}
