package com.timevale.drc.worker.service.sink;

import com.timevale.drc.base.Sink;
import com.timevale.drc.base.sinkConfig.CanalKafkaSinkConfig;
import com.timevale.drc.base.sinkConfig.KafkaSinkConfig;
import com.timevale.drc.base.sinkConfig.MySQLSinkConfig;
import com.timevale.drc.base.sinkConfig.RocketSinkConfig;

/**
 * 访问者模式.
 * @author gwk_2
 * @date 2022/3/14 18:04
 */
public interface InternalSinkFactory {

    <T> Sink<T> create(KafkaSinkConfig sinkConfig);

    <T> Sink<T> create(RocketSinkConfig sinkConfig);

    <T> Sink<T> create(MySQLSinkConfig sinkConfig);

    <T> Sink<T> create(CanalKafkaSinkConfig sinkConfig);
}
