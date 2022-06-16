package com.timevale.drc.worker.service.task.mysql.incr;

import com.timevale.drc.base.Task;
import com.timevale.drc.worker.deploy.Application;
import com.timevale.drc.worker.service.task.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.locks.LockSupport;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@Slf4j
public class DDLTaskTest {

    @Autowired
    TaskService taskService;

    @Test
    public void testSupportDDL() {
        try {
            Task task = taskService.getTask("loop_sync_20220606-1_Incr",true);
            MysqlIncrTask mysqlIncrTask = (MysqlIncrTask) task;

            taskService.updateTaskState(task, 1);

            task.start();

            // 永久阻塞
            LockSupport.park();
        } catch (Exception e) {
            log.info("testSupportDDL error",e);
        }
    }
}
