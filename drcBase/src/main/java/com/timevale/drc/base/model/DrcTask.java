package com.timevale.drc.base.model;

import com.timevale.drc.base.TaskTypeEnum;
import lombok.Data;

import javax.persistence.Table;

/**
 * @author gwk_2
 * @date 2021/1/29 00:10
 */
@Data
@Table(name = "drc_task")
public class DrcTask extends BaseDO {

    private String taskName;

    private String taskDesc;

    /**
     * @see TaskStateEnum
     */
    private Integer state;

    /**
     * @see TaskTypeEnum
     */
    private Integer taskType;

    /** 任务所属用户. 后期可根据该用户所属部门, 查看其它人是否有权限.*/
    private String userAlias;

    /** QPS 配置. */
    private Integer qpsLimitConfig;

    private String sinkJson;
}
