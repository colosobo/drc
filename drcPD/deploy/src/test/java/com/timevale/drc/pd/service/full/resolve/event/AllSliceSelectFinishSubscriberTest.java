package com.timevale.drc.pd.service.full.resolve.event;

import com.timevale.drc.base.dao.DrcSubTaskFullConfigMapper;
import com.timevale.drc.base.model.DrcSubTaskFullConfig;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class AllSliceSelectFinishSubscriberTest {

    @Mock
    private DrcSubTaskFullConfigMapper mapper;

    @Test
    public void onEvent() {
        MockitoAnnotations.initMocks(this);

        DrcSubTaskFullConfig model = new DrcSubTaskFullConfig();
        model.setDrcTaskId(0);
        model.setDbConfigId(0);
        model.setRangeSizeConfig(0);
        model.setTableName("");
        model.setSelectFieldList("");
        model.setSplitState(0);
        model.setSliceCount(0);
        model.setFinishSliceCount(0);
        model.setId(0);
        model.setCreateTime(new Date());
        model.setUpdateTime(new Date());
        model.setIsDeleted(0);

        when(mapper.selectByPrimaryKey(any())).thenReturn(model);
    }
}
