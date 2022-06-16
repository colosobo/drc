package com.timevale.drc.base;

import com.timevale.drc.base.sinkConfig.SinkConfig;

/**
 * @author gwk_2
 * @date 2021/4/22 14:05
 */
public interface SinkFactory {

    /**
     * 创建 Sink.
     * @param sinkConfig
     * @param <T>
     * @return sink
     */
    <T> Sink<T> create(SinkConfig sinkConfig);

}
