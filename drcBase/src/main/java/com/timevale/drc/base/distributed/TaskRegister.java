package com.timevale.drc.base.distributed;

import com.timevale.drc.base.Task;
import com.timevale.drc.base.Worker;

import java.util.List;

/**
 * task 注册中心.
 */
public interface TaskRegister extends DistributedRegister<Task> {

    Task registerWithWorker(String key, Worker worker);

    List<Task> getListFromWorker(Worker worker);

    @Override
    default void init(){};

    @Override
    default void stop(){};
}
