package com.timevale.drc.base;

/**
 * @author gwk_2
 * @date 2021/1/5 11:48
 */
public interface Router<D> {

    String getName();

    /**
     * 获取目标端点
     *
     * @return
     */
    D getRoute();

}
