package com.timevale.drc.base;


/**
 * @author gwk_2
 * @date 2021/1/28 22:14
 * @description
 */
public interface Worker {

    int id();

    /**
     * 获取地址.
     *
     * @return
     */
    Endpoint getEndpoint();

}
