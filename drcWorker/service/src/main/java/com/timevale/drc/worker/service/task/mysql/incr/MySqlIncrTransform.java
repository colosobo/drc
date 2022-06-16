package com.timevale.drc.worker.service.task.mysql.incr;

import com.timevale.drc.base.Transform;

/**
 * @author gwk_2
 * @date 2021/3/8 15:06
 */
public class MySqlIncrTransform<T> implements Transform<T> {
    @Override
    public T transform(T stringObjectMap) {
        // ignore
        return stringObjectMap;
    }
}
