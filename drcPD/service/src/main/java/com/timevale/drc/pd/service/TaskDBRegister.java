package com.timevale.drc.pd.service;

import com.timevale.drc.base.Router;
import com.timevale.drc.base.Task;
import com.timevale.drc.base.Worker;
import com.timevale.drc.base.alarm.AlarmUtil;
import com.timevale.drc.base.dao.DrcMachineRegisterTableMapper;
import com.timevale.drc.base.dao.DrcTaskRegisterTableMapper;
import com.timevale.drc.base.distributed.EndpointParser;
import com.timevale.drc.base.distributed.TaskRegister;
import com.timevale.drc.base.model.DrcMachineRegisterTable;
import com.timevale.drc.base.model.DrcTaskRegisterTable;
import com.timevale.drc.base.model.result.SelectIdleWorkerResult;
import com.timevale.drc.pd.service.exp.NotFoundWorkerException;
import com.timevale.drc.pd.service.task.PDSideRpcTask;
import com.timevale.drc.pd.service.worker.RpcWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TaskDBRegister implements TaskRegister {

    @Resource
    private DrcTaskRegisterTableMapper mapper;
    @Resource
    private DrcMachineRegisterTableMapper machineRegisterTableMapper;

    @Value("${env}")
    private String env;
    @Override
    public Task register(String key) {

        final DrcTaskRegisterTable old = mapper.selectByTaskName(key);
        if (old != null) {
            log.info("删除老的 task 映射, key = {}", key);
            mapper.deleteByTaskName(key);
        }

        DrcTaskRegisterTable mode = new DrcTaskRegisterTable();
        mode.setTaskName(key);
        // 空闲的 worker.
        final DrcMachineRegisterTable idleWorker = getIdleWorker();
        mode.setWorkerId(idleWorker.getId());
        mode.setWorkerIpPort(idleWorker.getIpPort());


        mapper.insertSelective(mode);

        return get(key);
    }

    @Override
    public boolean unRegister(String key) {
        return mapper.deleteByTaskName(key) > 0;
    }


    @Override
    public List<Task> list() {
        return mapper.selectAll().stream().map((Function<DrcTaskRegisterTable, Task>) mode ->
                        new PDSideRpcTask(
                                new InnerRouter(mode.getTaskName(), mode.getWorkerIpPort(), mode.getWorkerId())))
                .collect(Collectors.toList());
    }

    @Override
    public Task get(String key) {
        final DrcTaskRegisterTable mode = mapper.selectByTaskName(key);
        if (mode == null) {
            return null;
        }
        final DrcMachineRegisterTable db = machineRegisterTableMapper.selectByPrimaryKey(mode.getWorkerId());
        if (db == null) {
            // 机器没了.
            mapper.deleteByTaskName(key);
            return null;
        }
        return new PDSideRpcTask(
                new InnerRouter(mode.getTaskName(), mode.getWorkerIpPort(), mode.getWorkerId()));
    }

    @Override
    public Task registerWithWorker(String key, Worker worker) {
        DrcTaskRegisterTable mode = new DrcTaskRegisterTable();
        mode.setTaskName(key);
        mode.setWorkerId(worker.id());
        mode.setWorkerIpPort(worker.getEndpoint().getTcpUrl());
        mapper.insertSelective(mode);

        return get(key);
    }

    @Override
    public List<Task> getListFromWorker(Worker worker) {
        final long id = worker.id();
        final List<DrcTaskRegisterTable> list = mapper.selectByWorkerId(id);
        return list.stream().map((Function<DrcTaskRegisterTable, Task>) mode ->
                        new PDSideRpcTask(
                                new InnerRouter(
                                        mode.getTaskName(),
                                        mode.getWorkerIpPort(),
                                        mode.getWorkerId())))
                .collect(Collectors.toList());
    }

    public DrcMachineRegisterTable getIdleWorker() {
        final List<SelectIdleWorkerResult> list = mapper.selectTaskCountGroupByWorkerId();
        if (CollectionUtils.isEmpty(list)) {
            final List<DrcMachineRegisterTable> all = machineRegisterTableMapper.selectAllWorker();
            if (all.size() > 0) {
                return all.get(ThreadLocalRandom.current().nextInt(all.size()));
            } else {
                log.error("没有可用的 worker !!!!");
                AlarmUtil.pushAlarm2Admin(env + " 环境没有可用的 worker !!!!");
                throw new NotFoundWorkerException("没有可用的 worker !!!!");
            }
        }

        Collections.shuffle(list);

        int cou = Integer.MAX_VALUE;
        // count 会相同, 但不要紧.
        SelectIdleWorkerResult last = list.get(0);
        for (SelectIdleWorkerResult worker : list) {
            if (worker.getCount() < cou) {
                cou = worker.getCount();
                last = worker;
            }
        }
        return machineRegisterTableMapper.selectByPrimaryKey(last.getId());
    }

    static class InnerRouter implements Router<Worker> {

        String name;
        String ipPortProcess;
        Worker worker;

        public InnerRouter(String name, String ipPortProcess, Integer id) {
            this.name = name;
            this.ipPortProcess = ipPortProcess;
            this.worker = new RpcWorker(EndpointParser.fromIpPort(ipPortProcess), id);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Worker getRoute() {
            return worker;
        }
    }

}
