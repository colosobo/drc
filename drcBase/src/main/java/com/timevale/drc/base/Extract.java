package com.timevale.drc.base;

/**
 * @author gwk_2
 * @date 2021/1/28 22:12
 * @description
 */
public interface Extract<T> {

    T extract();

    void ack();
}
