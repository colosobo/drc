package com.timevale.drc.pd.service.worker;

import com.timevale.drc.base.Task;
import com.timevale.drc.base.Worker;
import com.timevale.drc.base.distributed.TaskRegister;
import com.timevale.drc.base.distributed.WorkerDistributedRegister;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author gwk_2
 * @date 2021/1/28 22:23
 */
@Component
@Slf4j
public class WorkerManager {

    @Resource
    private WorkerDistributedRegister workerDistributedRegister;
    @Resource
    private TaskRegister taskRegister;

    public List<Worker> getWorkerList() {
        return workerDistributedRegister.list();
    }

    public Worker getWorkerByWorkerName(String workerName) {
        return workerDistributedRegister.get(workerName);
    }


    public List<String> getTaskStringListByWorker(Worker worker) {
        final List<Task> list = taskRegister.getListFromWorker(worker);
        return list.stream().map(Task::getName).collect(Collectors.toList());
    }
}
