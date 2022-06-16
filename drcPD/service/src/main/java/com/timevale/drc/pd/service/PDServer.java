package com.timevale.drc.pd.service;

import com.timevale.drc.base.DefaultEndpoint;
import com.timevale.drc.base.Endpoint;
import com.timevale.drc.base.Task;
import com.timevale.drc.base.Worker;
import com.timevale.drc.base.redis.DrcLock;
import com.timevale.drc.base.redis.DrcLockFactory;
import com.timevale.drc.pd.service.exp.NotFoundWorkerException;
import com.timevale.drc.pd.service.stat.TaskMetricsService;
import com.timevale.drc.pd.service.task.TaskManager;
import com.timevale.drc.pd.service.user.HostNameUtil;
import com.timevale.drc.pd.service.worker.WorkerManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;

/**
 * 所有写操作,全部上锁.
 *
 * @author gwk_2
 * @date 2021/1/28 22:19
 */
@Component
@Getter
@Slf4j
public class PDServer {

    public String ip = HostNameUtil.getIp();

    @Value("${server.port}")
    private Integer port;

    private Endpoint selfEndpoint;
    @Autowired
    private RedissonClient redissonClient;
    @Resource
    private LoadBalancer<Worker, Task> loadBalancer;
    @Autowired
    private WorkerManager workerManager;
    @Autowired
    private TaskManager taskManager;
    @Autowired
    private DrcLockFactory lockFactory;

    private WorkerClusterState clusterState = WorkerClusterState.HEALTH;
    private TaskMetricsService taskMetricsService;

    @PostConstruct
    public void init() {
        selfEndpoint = new DefaultEndpoint(ip, port);
        taskMetricsService = new TaskMetricsService();
    }

    public Endpoint endpoint() {
        return selfEndpoint;
    }

    public WorkerClusterState clusterState() {
        return clusterState;
    }

    /**
     * 自动 reBalance.
     */
    public void autoReBalance(String cause) {
        List<Task> taskList = taskManager.getTaskList();
        List<Worker> workerList = workerManager.getWorkerList();
        reBalance(workerList, taskList, cause);
    }


    public void reBalance(List<Worker> workerList, List<Task> taskList, String cause) {
        DrcLock lock = lockFactory.getLock(LoadBalancer.DRC_PD_RE_BALANCE_LOCK_KEY);
        lock.lockAndProtect(30, () -> {
            try {
                clusterState = WorkerClusterState.RE_BALANCE;
                loadBalancer.reBalance(workerList, taskList, cause);
            } finally {
                clusterState = WorkerClusterState.HEALTH;
            }
        });
    }


    /**
     * 手动 failover
     *
     * @param workerName
     * @param taskName
     */
    public void failover(String taskName, String workerName) {
        DrcLock lock = lockFactory.getLock(LoadBalancer.DRC_PD_RE_BALANCE_LOCK_KEY);
        lock.lockAndProtect(3, () -> {
            try {
                clusterState = WorkerClusterState.FAILOVER;
                Worker worker = workerManager.getWorkerByWorkerName(workerName);

                Task task = taskManager.getTaskWithOutCreate(taskName);
                if (task == null) {
                    return;
                }

                loadBalancer.failover(worker, task);

            } finally {
                clusterState = WorkerClusterState.HEALTH;
            }
        });
    }


    public void startTask(String taskName) {
        Task task = taskManager.getTaskWithOutCreate(taskName);
        if (task == null) {
            DrcLock lock = lockFactory.getLock(LoadBalancer.DRC_PD_RE_BALANCE_LOCK_KEY);
            try {
                task = lock.lockAndProtect(3, () -> taskManager.registerTask(taskName));
            } catch (NotFoundWorkerException notFoundWorkerException) {
                // 没有找到任何一台可用的 worker, 可能性不大, 大概率是网络问题, 导致机器连不上.
                // 这时候, 先等待
                throw notFoundWorkerException;
            }
        }
        if (task == null) {
            throw new RuntimeException("注册失败 task. " + taskName);
        }
        try {
            task.start();
        } catch (Exception e) {
            log.warn("task 启动失败,taskName={},msg={} ", taskName, e.getMessage());
            taskManager.unRegisterTask(taskName);
            throw new RuntimeException(e);
        }
    }


    public void stopTask(String taskName, String cause) {
        DrcLock lock = lockFactory.getLock(LoadBalancer.DRC_PD_RE_BALANCE_LOCK_KEY);
        lock.lockAndProtect(3, () -> {
            Task task = taskManager.getTaskWithOutCreate(taskName);
            if (task != null) {
                task.stop(cause);
                taskManager.unRegisterTask(taskName);
            }
        });
    }

    public int getTotalQPS() {
        return taskMetricsService.getTotalQPS();
    }

    public int getQPS(String taskName) {
        return taskMetricsService.getQPS(taskName);
    }


}
