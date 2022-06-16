package com.timevale.drc.worker.service.task.mysql.full;

import com.timevale.drc.base.util.JdbcTemplateManager;

/**
 * @author gwk_2
 * @date 2021/3/10 14:00
 */
public interface SqlFormat {


    String select(String symbol1 );

    class SqlFormatFactory {
        public static SqlFormat create(JdbcTemplateManager.FieldType fieldType,
                                       String selectFieldList,
                                       String tableName,
                                       String pkName,
                                       String sliceMinPk,
                                       String sliceMaxPk,
                                       int limit,
                                       boolean isLast, String whereStatement) {
            // 如果是最后一个, 需要使用 <= 号.
            String symbol2 = isLast ? "<=" : "<";

            if (fieldType.findClass().equals(Integer.class)) {
                return new SqlFormatIntImpl(selectFieldList, tableName, pkName, sliceMinPk, sliceMaxPk, limit, symbol2, whereStatement);
            }
            if (fieldType.findClass().equals(String.class)) {
                return new SqlFormatStringImpl(selectFieldList, tableName, pkName, sliceMinPk, sliceMaxPk, limit, symbol2, whereStatement);
            }
            throw new RuntimeException("无法识别主键的类型.");
        }
    }
}
