package com.timevale.drc.pd.service.vo.req;

import com.timevale.drc.pd.service.vo.DbConfigVO;
import com.timevale.drc.pd.service.vo.DrcSubTaskIncrVO;
import com.timevale.drc.pd.service.vo.DrcTaskVO;
import com.timevale.drc.pd.service.vo.FullTaskConfigVO;
import lombok.Data;

/**
 * @author gwk_2
 * @date 2022/4/14 14:10
 */
@Data
public class MixDataBaseTaskReq {

    private DbConfigVO fullDbConfig;

    private DbConfigVO incrDbConfig;

    private DrcTaskVO drcTaskVO;

    /** 对于混合的类型, 这里可能直接就是表名 */
    private DrcSubTaskIncrVO drcSubTaskIncrVO;

    /** 全量同步的配置.  */
    private FullTaskConfigVO fullTaskConfigVO;
}
