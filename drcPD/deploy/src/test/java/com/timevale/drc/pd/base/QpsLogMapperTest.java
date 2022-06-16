package com.timevale.drc.pd.base;

import com.timevale.drc.base.dao.QpsLogMapper;
import com.timevale.drc.pd.deploy.Application;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class QpsLogMapperTest {

    @Autowired
    QpsLogMapper qpsLogMapper;

    @Test
    public void deleteWhenLessThanTime() {
        long s = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - TimeUnit.DAYS.toSeconds(1);
        int i = qpsLogMapper.deleteWhenLessThanTime(s);
        Assert.assertTrue(i >= 0);
    }
}
