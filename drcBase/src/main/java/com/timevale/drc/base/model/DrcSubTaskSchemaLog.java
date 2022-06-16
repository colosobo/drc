package com.timevale.drc.base.model;

import lombok.Data;

import javax.persistence.Table;

/**
 *
 * 库级别任务的详情 log
 */
@Data
@Table(name = "drc_sub_task_schema_log")
public class DrcSubTaskSchemaLog extends BaseDO {

    /** 父 task id */
    private Integer parentId;

    /** 表的总数. */
    private Integer tableTotal;

    /** 同步完成的分片的数量. */
    private Integer splitFinish;

    private String tableList;

    /** 表过滤表达式, 通常是 * or database.table1,database.table2 */
    private String tableExpression;

    /** 表拆分完成计数 */
    private Integer tableSplitFinish;

}
