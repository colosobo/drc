package com.timevale.drc.base.eventbus;

/**
 * @author gwk_2
 * @date 2022/4/19 19:21
 * @description
 * @see java.util.function.Consumer
 */
public interface DrcConsumer<T> {

    void accept(T t);
}
