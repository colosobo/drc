package com.timevale.drc.pd.service.full.resolve;

import com.timevale.drc.base.dao.DrcSubTaskFullConfigMapper;
import com.timevale.drc.pd.deploy.Application;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@Slf4j
public class SplitSliceTaskFactoryTest {

    @Autowired
    private SplitSliceTaskFactory splitSliceTaskFactory;
    @Autowired
    private DrcSubTaskFullConfigMapper drcSubTaskFullConfigMapper;

    @Test
    public void create() {
        SplitSliceTask splitSliceTask = splitSliceTaskFactory.create(drcSubTaskFullConfigMapper.select(null).get(0), null);
        Assert.assertNotNull(splitSliceTask);

    }
}
