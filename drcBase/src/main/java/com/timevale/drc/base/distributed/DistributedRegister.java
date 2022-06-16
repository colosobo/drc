package com.timevale.drc.base.distributed;

import java.util.List;

/**
 * 分布式注册中心
 *
 * @param <M> 要注册的对象.
 */
public interface DistributedRegister<M> {

    M register(String key);

    void init();

    void stop();

    boolean unRegister(String key);

    List<M> list();

    M get(String key);
}
