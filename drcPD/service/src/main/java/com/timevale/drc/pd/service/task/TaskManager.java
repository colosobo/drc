package com.timevale.drc.pd.service.task;

import com.timevale.drc.base.Task;
import com.timevale.drc.base.distributed.TaskRegister;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author gwk_2
 * @date 2021/1/28 22:39
 */
@Slf4j
@Component
public class TaskManager {

    @Resource
    private TaskRegister taskRegister;

    public List<Task> getTaskList() {
        return taskRegister.list();
    }

    public Task getTaskWithOutCreate(String taskName) {
        return taskRegister.get(taskName);
    }

    public Task registerTask(String name) {
        return taskRegister.register(name);
    }

    public void unRegisterTask(String name) {
        log.info("反向注册任务: {}", name);
        taskRegister.unRegister(name);
    }
}
