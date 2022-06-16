package com.timevale.drc.worker.service.task;

import com.timevale.drc.base.Task;
import com.timevale.drc.base.eventbus.Subscriber;
import com.timevale.drc.worker.deploy.Application;
import com.timevale.drc.worker.service.task.mysql.full.event.FullMySqlExtractOverEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@Slf4j
public class TaskServiceTest {

    @Autowired
    TaskService taskService;

    @Test
    public void update() {
        try {
            Task task = taskService.getTask("hello-test_FULL_6666");
            taskService.updateTaskState(task, 1);
        } catch (Exception e) {

        }
    }

    @Test
    public void getTask() {
        try {

//            Task task = taskService.getTask("hello-test_FULL_6");
//            task.start();

            final boolean[] f = {true};

            new ExtractOverSubscriberTest(() -> f[0] = false);

            while (f[0]) {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
            }
            System.out.println("getTask 结束....");

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    static class ExtractOverSubscriberTest extends Subscriber<FullMySqlExtractOverEvent> {

        Runnable runnable;

        public ExtractOverSubscriberTest(Runnable runnable) {
            super(FullMySqlExtractOverEvent.class);
            this.runnable = runnable;
        }

        @Override
        public void onEvent(FullMySqlExtractOverEvent event) {
            runnable.run();
        }
    }
}
