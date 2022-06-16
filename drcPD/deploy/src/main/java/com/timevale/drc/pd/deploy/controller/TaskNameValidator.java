package com.timevale.drc.pd.deploy.controller;

import com.timevale.drc.base.dao.DrcTaskMapper;
import com.timevale.drc.base.model.DrcTask;

public class TaskNameValidator {

    private final static String pattern = "^\\w{3,128}$";

    private DrcTaskMapper drcTaskMapper;

    public TaskNameValidator(DrcTaskMapper drcTaskMapper) {
        this.drcTaskMapper = drcTaskMapper;
    }

    public TaskNameValidator() {
    }

    public boolean valid(String taskName ) {

        if (drcTaskMapper != null) {
            DrcTask drcTask = drcTaskMapper.selectByName(taskName);
            if (drcTask != null) {
                throw new RuntimeException("任务名称已经存在, 不能重复, 请重新填写.");
            }
        }
        return true;
    }
}
