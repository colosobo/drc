package com.timevale.drc.worker.service.task.mysql.incr;

import com.timevale.drc.base.Transform;
import com.timevale.drc.base.binlog.Binlog2JsonModel;

/**
 * @author gwk_2
 * @date 2021/3/8 15:06
 */
public class MySqlIncrDrcModelTransform implements Transform<Binlog2JsonModel> {
    @Override
    public Binlog2JsonModel transform(Binlog2JsonModel binlog2JsonModel) {
        return binlog2JsonModel;
    }
}
