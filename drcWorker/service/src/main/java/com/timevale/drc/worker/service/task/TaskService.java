package com.timevale.drc.worker.service.task;

import com.ctrip.framework.apollo.ConfigService;
import com.timevale.drc.base.Task;
import com.timevale.drc.base.dao.DrcSubTaskFullSliceDetailMapper;
import com.timevale.drc.base.dao.DrcSubTaskIncrMapper;
import com.timevale.drc.base.eventbus.DrcThreadPool;
import com.timevale.drc.base.model.DrcSubTaskFullSliceDetail;
import com.timevale.drc.base.model.DrcSubTaskIncr;
import com.timevale.drc.worker.service.exp.NotFoundTaskException;
import com.timevale.drc.worker.service.task.mysql.TaskFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author gwk_2
 * @date 2021/1/29 14:39
 */
@Service
@Slf4j
public class TaskService {
    private final static Map<String, Task> CACHE = new ConcurrentHashMap<>();

    @Resource
    private DrcSubTaskFullSliceDetailMapper drcSubTaskFullSliceDetailMapper;
    @Resource
    private DrcSubTaskIncrMapper drcSubTaskIncrMapper;
    @Resource
    private TaskFactory taskFactory;

    @PostConstruct
    public void print() {
        DrcThreadPool.newScheduledThreadPool(1, TaskService.class.getCanonicalName()).scheduleAtFixedRate(() -> {
            if (ConfigService.getAppConfig().getBooleanProperty("TaskService.CACHE.print.enabled", false)) {
                log.info("cache size:{}, cache count:{}", CACHE.size(), CACHE.keySet());
            }
        }, 1, 3, TimeUnit.SECONDS);
    }


    public void updateTaskState(Task task, Integer state) {
        String name = task.getName();
        if (name.endsWith("Incr")) {
            drcSubTaskFullSliceDetailMapper.updateStateByName(state, task.getName());
        } else {
            drcSubTaskIncrMapper.updateStateByName(state, task.getName());
        }
    }

    public Collection<Task> getTaskList() {
        return CACHE.values();
    }

    public Task getTask(String name) {
        return getTask(name, false);
    }

    public Task getTask(String name, boolean create) {
        Task task = CACHE.get(name);
        if (task != null) {
            return task;
        }
        if (!create) {
            return null;
        }

        synchronized (this) {
            if ((task = CACHE.get(name)) != null) {
                return task;
            }

            if (name.endsWith("Incr")) {
                DrcSubTaskIncr drcSubTaskIncr = drcSubTaskIncrMapper.selectByName(name);
                if (drcSubTaskIncr == null) {
                    throw new NotFoundTaskException("找不到这个 task, name = " + name);
                }
                task = taskFactory.createIncrTask(drcSubTaskIncr.toBO());
            } else {
                DrcSubTaskFullSliceDetail drcSubTaskFullSliceDetail = drcSubTaskFullSliceDetailMapper.selectByName(name);
                task = taskFactory.createFullTask(drcSubTaskFullSliceDetail);

            }

            CACHE.put(name, task);
            return task;
        }
    }

    public void removeCache(Task task) {
        CACHE.remove(task.getName());
    }


}
