package com.timevale.drc.base;

import com.timevale.drc.base.eventbus.DrcConsumer;

/**
 * 核心领域模型.
 *
 * @author gwk_2
 * @date 2021/1/28 21:50
 */
public interface Task {

    /**
     * @see Task#start
     */
    void start();

    /**
     * 启动
     */
    default void start(DrcConsumer<Task> consumer) {
    }

    /**
     * 停止
     */
    void stop(String cause);

    /**
     * 是否在运行.
     *
     * @return
     */
    boolean isRunning();

    /**
     * 获取 task 指标.
     *
     * @return 指标.
     */
    default TaskMetrics metrics() {
        return TaskMetrics.Factory.create(0);
    }

    /**
     * 任务名称.
     *
     * @return
     */
    default String getName() {
        return getClass().getName();
    }

    /**
     * 获取状态.
     *
     * @return 状态.
     */
    default TaskStateEnum getState() {
        return TaskStateEnum.INIT;
    }

    /**
     * 获取日志
     *
     * @param line 行号.
     * @return
     */
    default String getLogText(int line) {
        return "";
    }
}
