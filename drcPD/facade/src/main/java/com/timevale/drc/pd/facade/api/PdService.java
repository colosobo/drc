package com.timevale.drc.pd.facade.api;

import com.timevale.drc.base.metrics.TaskMetricsModel;

import java.util.Map;

/**
 * @author gwk_2
 * @date 2021/1/28 21:14
 */
public interface PdService {

    /**
     * 取消注册此任务(针对的是"自动结束了任务,但任务没有取消注册").
     *
     * @param taskName
     * @return
     */
    String unRegister(String taskName);

    /**
     * 重启任务.
     *
     * @param taskName
     * @return success or fail
     */
    String restart(String taskName);

    /**
     * 上传 task 的 metrics 数据.
     * @param data 任务数据的集合.
     */
    String uploadTaskMetrics(Map<String, TaskMetricsModel> data);
}
