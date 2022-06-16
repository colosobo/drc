package com.timevale.drc.base.model;

import lombok.Data;

import javax.persistence.Table;
import java.sql.Date;

/**
 * 这些属性, 都是用户配置的.
 * @see  DrcSubTaskFullSliceDetail 是系统计算出来的.
 * @author gwk_2
 * @date 2021/3/8 11:29
 */
@Data
@Table(name = "drc_sub_task_full_config")
public class DrcSubTaskFullConfig extends BaseDO {

    public static final int SPLIT_STATE_NO = 0;
    public static final int SET_SPLIT_STATE_ING = 1;
    public static final int SPLIT_STATE_OVER = 2;

    /** 父 task id */
    private Integer drcTaskId;

    /** db 配置 id */
    private Integer dbConfigId;

    /** 配置的分片大小 */
    private Integer rangeSizeConfig;

    /** 表名 */
    private String tableName;

    /** 默认 * */
    private String selectFieldList;

    /** 拆分状态， 0未拆分， 1拆分中， 2已经拆分结束*/
    private Integer splitState;

    /** 拆分时间, 如果时间超过 5 分钟, 说明大概率是 JVM 宕机导致的, 5分钟可配置 */
    private Date splitTime;

    /** 拆分的分片总数，*/
    private Integer sliceCount;

    /** 已经 select 完的分片数字，修改这个数字，可能会有并发，注意 ABA 问题*/
    private Integer finishSliceCount;

//  private Integer oneSliceQpsLimitConfig;

    /**
     * where 语句.
     */
    private String whereStatement;

    private String sinkJson;

    public boolean splitOver() {
        return this.splitState == SPLIT_STATE_OVER;
    }
}
