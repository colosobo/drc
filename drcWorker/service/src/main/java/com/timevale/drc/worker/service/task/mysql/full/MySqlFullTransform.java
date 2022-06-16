package com.timevale.drc.worker.service.task.mysql.full;

import com.timevale.drc.base.Transform;
import com.timevale.drc.base.binlog.Binlog2JsonModel;

/**
 * @author gwk_2
 * @date 2021/3/8 15:06
 */
public class MySqlFullTransform implements Transform<Binlog2JsonModel> {
    @Override
    public Binlog2JsonModel transform(Binlog2JsonModel stringObjectMap) {
        // ignore
        return stringObjectMap;
    }
}
