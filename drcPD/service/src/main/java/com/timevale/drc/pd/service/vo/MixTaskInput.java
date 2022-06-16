package com.timevale.drc.pd.service.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * @author gwk_2
 * @date 2021/3/9 16:21
 */
@Data
@ApiModel
public class MixTaskInput {

    private DbConfigVO fullDbConfig;

    private DbConfigVO incrDbConfig;

    private DrcTaskVO drcTaskVO;

    /** 对于混合的类型, 这里可能直接就是表名 */
    private DrcSubTaskIncrVO drcSubTaskIncrVO;

    /** 全量同步的配置.  */
    private FullTaskConfigVO fullTaskConfigVO;

}
