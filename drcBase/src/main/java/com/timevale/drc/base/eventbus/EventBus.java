package com.timevale.drc.base.eventbus;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * 事件总线.
 *
 * @author 玄灭
 * @date 2018/11/13-上午10:03
 */
@Component
@Slf4j
public class EventBus {

    private static final
    ConcurrentHashMap<Class<? extends Event<?>>, CopyOnWriteArraySet<Subscriber<?>>> MAP =
            new ConcurrentHashMap<>();

    /**
     * 默认 cpu 核 * 2 线程数量.
     */
    private static final DrcThreadPoolExecutor EXECUTOR = DrcThreadPool.createThreadPool(EventBus.class.getSimpleName());

    private static final AtomicBoolean FLAG = new AtomicBoolean();

    static {
        EXECUTOR.prestartAllCoreThreads();
    }

    private EventBus() {
    }

    @PreDestroy
    public void destroy() {
        EXECUTOR.shutdown();
        FLAG.compareAndSet(false, true);
    }

    /**
     * 发布事件.
     *
     * @param event 事件.
     */
    public static <T> void post(Event<T> event) {
        if (FLAG.get()) {
            log.warn("EventBus always destroy, can not to be post event !");
            return;
        }
        if (event == null) {
            return;
        }
        CopyOnWriteArraySet<Subscriber<?>> set = MAP.get(event.getClass());
        if (set == null) {
            return;
        }
        for (Subscriber<?> subscribe : set) {
            if (subscribe.isSync()) {
                handleEvent(event, subscribe);
            } else {
                EXECUTOR.execute(() -> handleEvent(event, subscribe));
            }
            if (subscribe.nonrecurring()) {
                unRegister((Class<? extends Event<?>>) event.getClass(), subscribe);
            }
        }
    }

    /**
     * 批量发布事件.
     *
     * @param eventList 事件集合.
     */
    public static void post(List<Event<?>> eventList) {
        if (eventList == null || eventList.isEmpty()) {
            return;
        }
        for (Event<?> event : eventList) {
            post(event);
        }
    }

    /**
     * 订阅事件.
     *
     * @param eventClass 事件类型.
     * @param subscriber 订阅者.
     */
    public static void register(Class<? extends Event<?>> eventClass, Subscriber<?> subscriber) {
        CopyOnWriteArraySet<Subscriber<?>> set = getSubscribers(eventClass);
        set.add(subscriber);

        log.debug("subscriber : {} register  a event: {} ", subscriber, eventClass);
    }


    /**
     * 取消订阅事件.
     *
     * @param eventClass 事件类型.
     * @param subscriber 订阅者.
     */
    public static void unRegister(Class<? extends Event<?>> eventClass, Subscriber<?> subscriber) {
        CopyOnWriteArraySet<Subscriber<?>> set = getSubscribers(eventClass);
        set.remove(subscriber);
        log.debug("subscriber : {} unRegister  a event: {} ", subscriber, eventClass);
    }


    /**
     * 处理事件.
     *
     * @param event      事件.
     * @param subscriber 订阅者.
     */
    private static void handleEvent(final Event<?> event, final Subscriber subscriber) {
        try {
            subscriber.onEvent(event);
        } catch (Exception e) {
            log.error("subscriber {} handler event {} fail ", subscriber, event, e);
        }
    }

    private static CopyOnWriteArraySet<Subscriber<?>> getSubscribers(Class<? extends Event<?>> eventClass) {
        CopyOnWriteArraySet<Subscriber<?>> set = MAP.get(eventClass);
        if (set == null) {
            set = new CopyOnWriteArraySet<>();
            CopyOnWriteArraySet<Subscriber<?>> old = MAP.putIfAbsent(eventClass, set);
            if (old != null) {
                set = old;
            }

        }
        return set;
    }

}
