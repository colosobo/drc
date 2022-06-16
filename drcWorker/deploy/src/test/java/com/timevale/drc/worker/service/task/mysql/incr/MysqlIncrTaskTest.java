package com.timevale.drc.worker.service.task.mysql.incr;

import com.timevale.drc.base.dao.DrcSubTaskIncrMapper;
import com.timevale.drc.base.model.DrcSubTaskIncr;
import com.timevale.drc.worker.deploy.Application;
import com.timevale.drc.worker.service.task.mysql.DefaultTaskFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.locks.LockSupport;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class MysqlIncrTaskTest {

    @Autowired
    DefaultTaskFactory taskFactory;
    @Autowired
    private DrcSubTaskIncrMapper drcSubTaskIncrMapper;


    @Test
    public void start1() {

        DrcSubTaskIncr drcSubTaskIncr2 = drcSubTaskIncrMapper.selectByPrimaryKey(158);
        MysqlIncrTask  mysqlIncrTask2 = taskFactory.createIncrTask(drcSubTaskIncr2.toBO());
        mysqlIncrTask2.start();

        LockSupport.park();
    }

    @Test
    public void start() {
        DrcSubTaskIncr drcSubTaskIncr = drcSubTaskIncrMapper.selectByPrimaryKey(65);
        MysqlIncrTask    mysqlIncrTask = taskFactory.createIncrTask(drcSubTaskIncr.toBO());
        mysqlIncrTask.doStart();
        mysqlIncrTask.stop("");
    }
}
