package com.timevale.drc.pd.service.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 全量同步对象.
 *
 * @author gwk_2
 * @date 2021/3/9 16:18
 */
@Data
@ApiModel
public class FullTaskInput {

    private DrcTaskVO drcTaskVO;

    private FullTaskConfigVO fullTaskConfigVO;

    private DbConfigVO dbConfigVO;
}
