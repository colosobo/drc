package com.timevale.drc.pd.service;

import com.timevale.drc.base.dao.DrcSubTaskFullSliceDetailMapper;
import com.timevale.drc.base.dao.DrcSubTaskIncrMapper;
import com.timevale.drc.base.model.BaseTaskModel;
import com.timevale.drc.base.model.DrcSubTaskFullSliceDetail;
import com.timevale.drc.base.model.DrcSubTaskIncr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * task db 操作者。
 */
@Component
public class TaskDbOperator {

    @Autowired
    private DrcSubTaskFullSliceDetailMapper fullMapper;
    @Autowired
    private DrcSubTaskIncrMapper incrMapper;

    public BaseTaskModel lookup(String taskName) {
        boolean incr = taskName.endsWith("Incr");

        BaseTaskModel model = null;
        if (incr) {
            model = incrMapper.selectByName(taskName);
        } else {
            model = fullMapper.selectByName(taskName);
        }

        if (model == null) {
            throw new RuntimeException("task 不存在. taskName=" + taskName);
        }
        return model;
    }


    public int updateByPrimaryKeySelective(BaseTaskModel baseTaskModel) {
        if (baseTaskModel instanceof DrcSubTaskFullSliceDetail) {
            return fullMapper.updateByPrimaryKeySelective((DrcSubTaskFullSliceDetail) baseTaskModel);
        }
        if (baseTaskModel instanceof DrcSubTaskIncr) {
            return incrMapper.updateByPrimaryKeySelective((DrcSubTaskIncr) baseTaskModel);
        }
        throw new RuntimeException("类型不存在。");

    }
}
