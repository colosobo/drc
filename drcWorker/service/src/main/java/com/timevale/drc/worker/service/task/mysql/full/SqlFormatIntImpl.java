package com.timevale.drc.worker.service.task.mysql.full;

/**
 * @author gwk_2
 * @date 2021/3/10 14:00
 */
public class SqlFormatIntImpl implements SqlFormat {

    String selectFieldList;
    String tableName;
    String pkName;
    String sliceMinPk;
    String sliceMaxPk;
    int limit;
    String symbol2;
    String whereStatement;

    public SqlFormatIntImpl(String selectFieldList,
                            String tableName,
                            String pkName,
                            String sliceMinPk,
                            String sliceMaxPk,
                            int limit,
                            String symbol2,
                            String whereStatement
    ) {
        this.selectFieldList = selectFieldList;
        this.tableName = tableName;
        this.pkName = pkName;
        this.sliceMinPk = sliceMinPk;
        this.sliceMaxPk = sliceMaxPk;
        this.limit = limit;
        this.symbol2 = symbol2;
        this.whereStatement = whereStatement;
    }

    @Override
    public String select(String symbol1) {
        // select * from table where id >= 0 and id < 100 order by id limit 1000
        return "select " + selectFieldList + " from " + tableName +
                " where " + pkName + symbol1 + sliceMinPk + " and " + pkName + symbol2 + sliceMaxPk +
                " " + whereStatement + " order by " + pkName + " limit " + this.limit;
    }

}
