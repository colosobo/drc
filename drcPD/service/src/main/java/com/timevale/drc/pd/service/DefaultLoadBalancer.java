
package com.timevale.drc.pd.service;

import com.timevale.drc.base.Task;
import com.timevale.drc.base.TaskStateEnum;
import com.timevale.drc.base.Worker;
import com.timevale.drc.base.eventbus.DrcThreadPool;
import com.timevale.drc.base.eventbus.DrcThreadPoolExecutor;
import com.timevale.drc.base.redis.DrcLock;
import com.timevale.drc.base.redis.DrcLockFactory;
import com.timevale.drc.base.util.Cost;
import com.timevale.drc.pd.service.task.TaskManager;
import com.timevale.drc.pd.service.worker.WorkerManager;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author gwk_2
 * @date 2021/1/28 23:04
 * @description
 */
@Component
@Slf4j
@Setter
public class DefaultLoadBalancer implements LoadBalancer<Worker, Task> {

    @Autowired
    private WorkerManager workerManager;
    @Autowired
    private TaskManager taskManager;
    @Autowired
    private DrcLockFactory lockFactory;
    @Autowired
    private TaskDBRegister taskDBRegister;

    final DrcThreadPoolExecutor doReBalancePool = DrcThreadPool.createThreadPool("doReBalance", 100, 100);

    /**
     * 1. 找到所有 worker, 区分健康的和损坏的, 找到坏的 worker 上的 orphanTaskList.
     * 2. 找到所有的 taskList; taskList.length / worker.length = avg, 可能除不尽; 每台 worker 上应该承受多少机器.
     * 3. 遍历每个 worker, 如果 worker task 数量 < avg, 就启动 orphanTaskList;
     * ------------------ 如果 worker task 数量 > avg + 1, 就关闭 task, 并存入 orphanTaskList;
     * 4. 如果除不尽, 将剩下 orphanTaskList 遍历, 逐个在 workerList 上启动.
     *
     * @param workList 健康的 worker
     * @param taskList 所有的 Task
     */
    private void doReBalance(List<Worker> workList, List<Task> taskList, String cause) {

        Cost cost = Cost.start();

        //workList = filterHealthWorker(workList);

        final LinkedList<String> orphanTaskList = orphanTaskList(taskList);
        if (orphanTaskList.size() == 0) {
            return;
        }

        int avg = taskList.size() / workList.size();
        int mod = taskList.size() % workList.size();
        log.warn("avg = {}, mod ={}, workerSiz={}, orphanTaskList = {}", avg, mod, workList, orphanTaskList);

        Collections.shuffle(workList);

        for (Worker worker : workList) {
            final List<String> list = workerManager.getTaskStringListByWorker(worker);
            boolean workerFail = false;

            while (list.size() < avg) {
                final String first = orphanTaskList.peek();
                if (first == null) {
                    log.info("first is null");
                    break;
                }
                Task task = taskManager.getTaskWithOutCreate(first);
                if (task == null) {
                    task = taskManager.registerTask(first);
                }
                try {
                    task.start();
                } catch (Exception e) {
                    // 可能会挂掉.
                    orphanTaskList.addAll(list);
                    workerFail = true;
                    break;
                }
                log.info("start 1 {} success", first);
                list.add(orphanTaskList.poll());
            }

            int avg2 = avg;
            if (mod > 0) {
                avg2 = avg + 1;
            }

            while (list.size() > avg2 && !workerFail) {
                Task task = taskManager.getTaskWithOutCreate(list.get(0));
                if (task == null) {
                    log.info("task is null {}", list.get(0));
                    list.remove(list.get(0));
                    continue;
                }
                try {
                    task.stop(cause + "_DefaultLoadBalancer_STOP");
                    log.info("stop {} success", list.get(0));
                } catch (Exception e) {
                    // 这个 worker 大概率挂机了;
                    log.error("worker 大概率挂机了, taskName = {}, msg={}", list.get(0), e.getMessage());
                    orphanTaskList.addAll(list);
                    break;
                }
                taskManager.unRegisterTask(task.getName());
                orphanTaskList.addLast(list.get(0));
                list.remove(list.get(0));
            }

        }

        Collections.shuffle(workList);

        // 再来一遍, mod
        while (orphanTaskList.size() > 0) {
            final String first = orphanTaskList.peek();
            Task task = taskManager.getTaskWithOutCreate(first);
            if (task == null) {
                task = taskManager.registerTask(first);
            }
            task.start();
            log.info("start 2 {} success", first);
            orphanTaskList.poll();
        }
        log.info("cost = {}ms", cost.end());
    }


    private LinkedList<String> orphanTaskList(List<Task> taskList) {
        Map<String, Future<TaskStateEnum>> map = new HashMap<>();

        CountDownLatch latch = new CountDownLatch(taskList.size());

        for (Task task : taskList) {
            if (task == null) {
                continue;
            }
            map.put(task.getName(), doReBalancePool.submit(() -> {
                try {
                    return task.getState();
                } catch (Exception e) {
                    return TaskStateEnum.EXCEPTION;
                } finally {
                    try {
                        latch.countDown();
                    } catch (Exception e) {
                        //
                    }
                }
            }));
        }

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // ignore
        }


        LinkedList<String> orphanTaskList = new LinkedList<>();

        for (String name : map.keySet()) {
            try {
                if (map.get(name).get(3, TimeUnit.SECONDS) == TaskStateEnum.EXCEPTION) {
                    orphanTaskList.add(name);
                }
            } catch (InterruptedException e) {
                // ignore
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                // ignore
                log.warn(e.getMessage(), e);
            }
        }

        return orphanTaskList;
    }

    @Override
    public void reBalance(List<Worker> workList, List<Task> taskList, String cause) {
        DrcLock lock = lockFactory.getLock(DRC_PD_RE_BALANCE_LOCK_KEY);
        lock.lockAndProtect(30, () -> doReBalance(workList, taskList, cause));
    }

    @Override
    public void failover(Worker worker, Task task) {
        task.stop("failover");
        taskDBRegister.unRegister(task.getName());
        final Task register = taskDBRegister.registerWithWorker(task.getName(), worker);
        register.start();
    }

    @Data
    @AllArgsConstructor
    static class Pair<A, B> {
        A a;
        B b;
    }

}
