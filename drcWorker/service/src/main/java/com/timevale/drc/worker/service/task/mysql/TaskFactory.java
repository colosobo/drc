package com.timevale.drc.worker.service.task.mysql;

import com.timevale.drc.base.Task;
import com.timevale.drc.base.model.DrcSubTaskFullSliceDetail;
import com.timevale.drc.base.model.bo.DrcSubTaskIncrBO;

/**
 * @author gwk_2
 * @date 2022/1/25 11:56
 * @description
 */
public interface TaskFactory {

    /**
     * 创建全量任务
     * @param subTask
     * @return
     */
    Task createFullTask(DrcSubTaskFullSliceDetail subTask);

    /**
     * 创建增量任务
     *
     * @param drcSubTaskIncr
     * @return
     */
    Task createIncrTask(DrcSubTaskIncrBO drcSubTaskIncr);
}
