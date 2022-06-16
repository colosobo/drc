package com.timevale.drc.base;

import java.util.List;

/**
 * 定义 sink 接口.
 *
 * @author gwk_2
 * @date 2021/1/28 22:12
 */
public interface Sink<T> {

    /**
     * 开始 Sink
     */
    void start();

    /**
     * 停止 sink
     */
    void stop();

    /**
     * 当前. 大部分情况下,都只有一条数据, 只有在批量 sink MySQL 时,才会有多条数据.
     * @param t
     */
    void sink(List<T> t);

    default String getName(){
        return getClass().getCanonicalName();
    }

}
