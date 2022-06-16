package com.timevale.drc.worker.service.task.mysql.full.event;

import com.timevale.drc.base.eventbus.Event;

/**
 * 反注册事件.
 * @author gwk_2
 * @date 2021/3/23 23:44
 */
public class UnRegisterEvent implements Event<String> {

    private final String subTaskName;

    public UnRegisterEvent(String subTaskName) {
        this.subTaskName = subTaskName;
    }

    @Override
    public String data() {
        return subTaskName;
    }
}
