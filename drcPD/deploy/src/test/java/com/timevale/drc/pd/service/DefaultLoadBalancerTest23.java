package com.timevale.drc.pd.service;

import com.google.common.collect.Lists;
import com.timevale.drc.base.*;
import com.timevale.drc.base.redis.DrcLock;
import com.timevale.drc.base.redis.DrcLockFactory;
import com.timevale.drc.pd.service.task.TaskManager;
import com.timevale.drc.pd.service.worker.WorkerManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

public class DefaultLoadBalancerTest23 {

    @Mock
    private WorkerManager workerManager;

    private TaskManager taskManager = new TaskManager() {
        @Override
        public void unRegisterTask(String name) {
            for (List<String> value : map.values()) {
                value.remove(name);
            }
        }

        @Override
        public Task getTaskWithOutCreate(String name) {
            return new TestTask(TaskStateEnum.RUNNING, name);
        }
    };
    @Mock
    private DrcLockFactory lockFactory;

    DefaultLoadBalancer newDefaultLoadBalancer = new DefaultLoadBalancer();

    List<Task> taskList;

    List<Worker> workerList;

    Worker worker1 = new TestWorker(DefaultEndpoint.buildFromIpPort("1.1.1.1:8080"), true);
    Worker worker2 = new TestWorker(DefaultEndpoint.buildFromIpPort("1.1.1.1:8081"), true);
    Worker worker3 = new TestWorker(DefaultEndpoint.buildFromIpPort("1.1.1.1:8082"), true);
    Worker worker4 = new TestWorker(DefaultEndpoint.buildFromIpPort("1.1.1.1:8083"), false);

    @Before
    public void b() {
        MockitoAnnotations.initMocks(this);

        newDefaultLoadBalancer.setWorkerManager(workerManager);
        newDefaultLoadBalancer.setTaskManager(taskManager);
        newDefaultLoadBalancer.setLockFactory(lockFactory);


        when(workerManager.getTaskStringListByWorker(worker1)).thenReturn(Lists.newArrayList("11", "1x"));
        when(workerManager.getTaskStringListByWorker(worker2)).thenReturn(Lists.newArrayList("22", "2x", "2xx", "2xxx"));
        when(workerManager.getTaskStringListByWorker(worker3)).thenReturn(Lists.newArrayList("33", "3x", "3xx", "3xxx", "3xxxx", "3xxxxxx", "333", "3333"));
        when(workerManager.getTaskStringListByWorker(worker4)).thenReturn(Lists.newArrayList("44", "4x"));


        when(lockFactory.getLock(LoadBalancer.DRC_PD_RE_BALANCE_LOCK_KEY)).thenReturn(new DrcLock(null, null) {
            @Override
            public void lockAndProtect(int waitTimeInSec, Runnable callable) {
                callable.run();
            }
        });


        taskList = Lists.newArrayList(
                new TestTask(TaskStateEnum.RUNNING, "11"),
                new TestTask(TaskStateEnum.RUNNING, "1x"),
                new TestTask(TaskStateEnum.RUNNING, "22"),
                new TestTask(TaskStateEnum.RUNNING, "2x"),
                new TestTask(TaskStateEnum.RUNNING, "2xx"),
                new TestTask(TaskStateEnum.RUNNING, "2xxx"),
                new TestTask(TaskStateEnum.RUNNING, "33"),
                new TestTask(TaskStateEnum.RUNNING, "3x"),
                new TestTask(TaskStateEnum.RUNNING, "3xx"),
                new TestTask(TaskStateEnum.RUNNING, "3xxx"),
                new TestTask(TaskStateEnum.RUNNING, "3xxxx"),
                new TestTask(TaskStateEnum.RUNNING, "3xxxxx"),
                new TestTask(TaskStateEnum.RUNNING, "333"),
                new TestTask(TaskStateEnum.RUNNING, "3333"),
                new TestTask(TaskStateEnum.EXCEPTION, "44"),
                new TestTask(TaskStateEnum.EXCEPTION, "4x")
        );

        workerList = Lists.newArrayList(
                worker1, worker2, worker3
        );


    }

    @Test
    public void reBalance() {
        System.out.println("workerList = " + workerList.size());
        System.out.println("taskList   = " + taskList.size());
        System.out.println("avg = " + (taskList.size() / workerList.size()));
        System.out.println("mod = " + (taskList.size() % workerList.size()));

        map.put(worker1.getEndpoint().getTcpUrl(), Lists.newArrayList("11", "1x"));
        map.put(worker2.getEndpoint().getTcpUrl(), Lists.newArrayList("22", "2x", "2xx", "2xxx"));
        map.put(worker3.getEndpoint().getTcpUrl(), Lists.newArrayList("33", "3x", "3xx", "3xxx", "3xxxx", "3xxxxxx", "333", "3333"));

        newDefaultLoadBalancer.reBalance(workerList, taskList, "test");
        System.out.println(map);
    }

    static Map<String, List<String>> map = new HashMap<>();

    class TestWorker implements Worker {

        Endpoint endpoint;
        boolean health;

        public TestWorker(Endpoint endpoint, boolean health) {
            this.endpoint = endpoint;
            this.health = health;
        }

        @Override
        public int id() {
            return 0;
        }

        @Override
        public Endpoint getEndpoint() {
            return endpoint;
        }

    }


    class TestTask implements Task {

        TaskStateEnum taskStateEnum;
        String name;

        public TestTask(TaskStateEnum taskStateEnum, String name) {
            this.taskStateEnum = taskStateEnum;
            this.name = name;
        }

        public TestTask(TaskStateEnum taskStateEnum) {
            this.taskStateEnum = taskStateEnum;
        }

        @Override
        public TaskStateEnum getState() {
            return taskStateEnum;
        }


        @Override
        public String getName() {
            return name;
        }

        @Override
        public void start() {

        }

        @Override
        public void stop(String cause) {

        }


        @Override
        public boolean isRunning() {
            return false;
        }
    }
}
