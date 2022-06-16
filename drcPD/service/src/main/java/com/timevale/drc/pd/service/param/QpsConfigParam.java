package com.timevale.drc.pd.service.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

/**
 * @author gwk_2
 * @date 2021/3/18 15:38
 */
@Data
@ToString
@ApiModel
public class QpsConfigParam {

    @ApiModelProperty("全局QPS")
    private Integer qps;

    @ApiModelProperty("任务名称")
    private String taskName;
}
