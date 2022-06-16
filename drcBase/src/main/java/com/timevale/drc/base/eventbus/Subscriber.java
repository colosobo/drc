package com.timevale.drc.base.eventbus;

/**
 * 事件订阅者.
 *
 * @author 玄灭
 */
public abstract class Subscriber<E extends Event<?>> {

    protected Subscriber(Class<E> c) {
        EventBus.register(c, this);
    }

    /**
     * 发生事件时回调此方法.
     *
     * @param event
     */
    public abstract void onEvent(E event);

    /**
     * 同步处理还是异步处理, 注意: 同步处理将会影响框架吞吐量.
     */
    public boolean isSync() {
        return false;
    }

    /**
     * 是否只使用一次?
     * @return true 仅一次, false:循环使用.
     */
    protected boolean nonrecurring() {
        return false;
    }
}
