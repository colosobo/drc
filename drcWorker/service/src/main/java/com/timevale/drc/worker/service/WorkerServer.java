package com.timevale.drc.worker.service;

import com.timevale.drc.base.*;
import com.timevale.drc.base.dao.DrcTaskMapper;
import com.timevale.drc.base.eventbus.DrcRejectedException;
import com.timevale.drc.base.rpc.HostNameUtil;
import com.timevale.drc.base.serialize.GenericJackson2JsonSerializer;
import com.timevale.drc.base.util.LogUtil;
import com.timevale.drc.worker.service.task.TaskService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * @author gwk_2
 * @date 2021/1/28 23:46
 * @see Worker
 */
@Service
@Getter
@Slf4j
public class WorkerServer {
    private static final GenericJackson2JsonSerializer SERIALIZER = new GenericJackson2JsonSerializer();

    @Value("${server.port}")
    public Integer port;

    @Autowired
    private TaskService taskService;
    @Autowired
    private DrcTaskMapper drcTaskMapper;

    private Endpoint selfEndpoint;

    @PostConstruct
    public void init() {
        String ip = HostNameUtil.getIp();
        selfEndpoint = new DefaultEndpoint(ip, port);
    }

    @PreDestroy
    public void destroy() {
    }

    public void start(String taskName) {
        Task task = taskService.getTask(taskName, true);
        if (task.getState() == TaskStateEnum.RUNNING) {
            log.warn("不要重复启动....{}", taskName);
            return;
        }
        try {
            task.start(o -> taskService.removeCache(o));
        } catch (DrcRejectedException e) {
            task.stop("start fail");
            taskService.removeCache(task);
            task = taskService.getTask(taskName, true);
            task.start(o -> taskService.removeCache(o));
        }
    }

    public void stop(String taskName, String cause) {
        Task task = taskService.getTask(taskName, false);
        if (task == null) {
            return;
        }
        task.stop(cause);
        taskService.updateTaskState(task, TaskStateEnum.HAND_STOP.code);
        taskService.removeCache(task);
    }

    public TaskStateEnum getState(String taskName) {
        Task task = taskService.getTask(taskName, false);
        if (task == null) {
            return TaskStateEnum.UN_KNOW;
        }
        return task.getState();
    }


    public boolean isRunning(String taskName) {
        Task task = taskService.getTask(taskName, false);
        if (task == null) {
            return false;
        }
        return task.isRunning();
    }

    public String getLogText(String taskName, int line) {
        return LogUtil.getLogText(taskName, line);
    }
}
