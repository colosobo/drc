package com.timevale.drc.worker.service.task;

/**
 *
 * 协调器.
 *
 * @author gwk_2
 * @date 2021/3/11 15:51
 * @description
 */
public interface Coordinator {

    String OVER_TIPS = "OVER";

    /**
     * 监听某个 task 的事件.
     *
     * @param parentTaskName 任务名.
     * @param callback 全量同步结束后, 需要执行的回调.
     */
    void watch(String parentTaskName, Runnable callback);

    /**
     * 反监听.
     * @param parentTaskName 任务名.
     */
    void unWatch(String parentTaskName);

    /**
     * 发布某个 task 的协调事件.
     *
     * @param parentTaskName 任务名.
     */
    void publish(String parentTaskName);
}
