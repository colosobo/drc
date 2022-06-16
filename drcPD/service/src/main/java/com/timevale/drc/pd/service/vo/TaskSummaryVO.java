package com.timevale.drc.pd.service.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author gwk_2
 * @date 2021/3/18 13:34
 */
@Data
@ApiModel("task 概览")
public class TaskSummaryVO {

    @ApiModelProperty("task 总数")
    private Integer count;

    @ApiModelProperty("全量任务总数")
    private Integer fullCount;

    @ApiModelProperty("增量任务总数")
    private Integer incrCount;

    @ApiModelProperty("数据库运行中的增量任务总数")
    private Integer dbIncrCount;

    @ApiModelProperty("任务总数(包含异常)")
    private Integer all;
}
