package com.timevale.drc.pd.service.full.resolve;

import com.timevale.drc.base.dao.*;
import com.timevale.drc.base.model.DrcSubTaskFullConfig;
import com.timevale.drc.pd.deploy.Application;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@Slf4j
public class SplitSliceTaskTest {


    @Autowired
    private DrcTaskMapper drcTaskMapper;
    @Autowired
    private DrcDbConfigMapper drcDbConfigMapper;
    @Autowired
    private DrcSubTaskFullConfigMapper fullConfigMapper;

    private DrcSubTaskFullConfig fullConfig;

    @Autowired
    private DrcSubTaskFullSliceDetailMapper drcSubTaskFullSliceDetailMapper;
    @Autowired
    DrcSubTaskSchemaLogMapper drcSubTaskSchemaLogMapper;

    private Integer limit = 10;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        //tableSplitResolver.splitSlice(any(), anyInt(), anyString(), anyString(), anyString(), anyString(), any(), any(), any(), any()));

    }

    @Test
    public void run() {
        List<DrcSubTaskFullConfig> select = fullConfigMapper.select(null);
        fullConfig = select.get(0);
        SplitSliceTask splitSliceTaskT = new SplitSliceTask(null,
                fullConfigMapper,
                drcTaskMapper,
                fullConfig, drcDbConfigMapper,
                drcSubTaskFullSliceDetailMapper,
                drcSubTaskSchemaLogMapper, limit);
        try {
            splitSliceTaskT.run();
        } catch (Exception e) {
        }
    }
}
