package com.timevale.drc.pd.service.param;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.ToString;

/**
 * @author gwk_2
 * @date 2021/5/31 11:01
 */
@Data
@ToString
@ApiModel
public class OpenApiStopParams {
    /**
     * 任务名称.
     */
    String task;

    public OpenApiStopParams() {
    }

    public OpenApiStopParams(String task) {
        this.task = task;
    }
}
