package com.timevale.drc.worker.service.sink;

import com.timevale.drc.base.Sink;
import com.timevale.drc.base.SinkFactory;
import com.timevale.drc.base.sinkConfig.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author gwk_2
 * @date 2021/4/22 14:07
 */
@Component
public class DefaultSinkFactory implements SinkFactory {

    @Autowired
    private InternalSinkFactory internalSinkFactory;

    /**
     * @param sinkConfig config
     * @param <T>
     * @return
     */
    @Override
    public <T> Sink<T> create(SinkConfig sinkConfig) {
        if (sinkConfig instanceof KafkaSinkConfig) {
            return internalSinkFactory.create((KafkaSinkConfig) sinkConfig);
        }
        if (sinkConfig instanceof RocketSinkConfig) {
            return internalSinkFactory.create((RocketSinkConfig) sinkConfig);
        }
        if (sinkConfig instanceof MySQLSinkConfig) {
            return internalSinkFactory.create((MySQLSinkConfig) sinkConfig);
        }
        if (sinkConfig instanceof CanalKafkaSinkConfig) {
            return internalSinkFactory.create((CanalKafkaSinkConfig) sinkConfig);
        }

        throw new RuntimeException("无法识别 SinkConfig 类型, 类型 =" + sinkConfig.getClass().getName());

    }
}
