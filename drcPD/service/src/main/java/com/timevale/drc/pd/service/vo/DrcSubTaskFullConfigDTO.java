package com.timevale.drc.pd.service.vo;

import com.timevale.drc.base.model.DrcSubTaskFullConfig;
import lombok.Data;

import java.sql.Date;

/**
 * @see DrcSubTaskFullConfig
 * @author gwk_2
 * @date 2022/4/14 22:08
 */
@Data
public class DrcSubTaskFullConfigDTO {

    private Long id;

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

    private boolean start;

}
