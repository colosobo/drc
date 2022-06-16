package com.timevale.drc.pd.service.full.resolve;

import com.timevale.drc.base.dao.*;
import com.timevale.drc.base.model.DrcSubTaskFullConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author gwk_2
 * @date 2021/3/8 11:01
 */
@Component
public class SplitSliceTaskFactory {

    @Autowired
    private DrcTaskMapper drcTaskMapper;
    @Autowired
    private DrcDbConfigMapper drcDbConfigMapper;
    @Autowired
    private DrcSubTaskFullConfigMapper drcSubTaskFullConfigMapper;
    @Autowired
    private DrcSubTaskFullSliceDetailMapper drcSubTaskFullSliceDetailMapper;
    @Autowired
    private DrcSubTaskSchemaLogMapper drcSubTaskSchemaLogMapper;

    @Value("${SplitSliceTask.limit:1000}")
    private Integer limit;

    public SplitSliceTask create(DrcSubTaskFullConfig drcSubTaskFullConfig, SelectFullTaskScheduler selectFullTaskScheduler) {
        return new SplitSliceTask(o -> selectFullTaskScheduler.startOne(),
                drcSubTaskFullConfigMapper, drcTaskMapper, drcSubTaskFullConfig, drcDbConfigMapper,
                drcSubTaskFullSliceDetailMapper, drcSubTaskSchemaLogMapper, limit);
    }
}
