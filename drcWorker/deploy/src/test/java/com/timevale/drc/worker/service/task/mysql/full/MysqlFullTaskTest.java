package com.timevale.drc.worker.service.task.mysql.full;

import com.timevale.drc.base.dao.DrcSubTaskFullSliceDetailMapper;
import com.timevale.drc.base.model.DrcSubTaskFullSliceDetail;
import com.timevale.drc.worker.deploy.Application;
import com.timevale.drc.worker.service.task.mysql.DefaultTaskFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class MysqlFullTaskTest  {

    @Autowired
    DefaultTaskFactory taskFactory;
    @Autowired
    private DrcSubTaskFullSliceDetailMapper drcSubTaskFullSliceDetailMapper;

    @Test
    public void start() {

        DrcSubTaskFullSliceDetail drcSubTaskFullSliceDetail = drcSubTaskFullSliceDetailMapper.selectByPrimaryKey(13330);
        if (drcSubTaskFullSliceDetail == null) {
            return;
        }
        MysqlFullTask mysqlFullTask = taskFactory.createFullTask(drcSubTaskFullSliceDetail);
        mysqlFullTask.setRunning();
        mysqlFullTask.doStart();
    }

    @Test
    public void main() {
        String taskName = "legal_seal_auth_011105_full_0";
        System.out.println(taskName.substring(0, taskName.lastIndexOf("_full_")));
    }
}
