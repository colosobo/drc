package com.timevale.drc.pd.service.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 增量同步对象.
 *
 * @author gwk_2
 * @date 2021/3/9 16:18
 */
@Data
@ApiModel
public class IncrTaskInput {

    private DrcTaskVO drcTaskVO;

    @ApiModelProperty("")
    private DrcSubTaskIncrVO drcSubTaskIncrVO;

    private DbConfigVO dbConfigVO;
}
