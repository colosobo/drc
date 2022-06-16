package com.timevale.drc.pd.service.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

/**
 * @author gwk_2
 * @date 2021/5/31 11:01
 */
@Data
@ToString
@ApiModel
public class OpenApiTaskDetailParams {
    /**
     * 父任务名称.
     */
    @ApiModelProperty("父任务名称")
    String task;

}
