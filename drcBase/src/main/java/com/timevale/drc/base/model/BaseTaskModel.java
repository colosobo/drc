package com.timevale.drc.base.model;

import com.timevale.drc.base.BaseTask;
import lombok.Data;

@Data
public class BaseTaskModel extends BaseDO {

    protected String subTaskName;

    protected Integer parentId;
    /**
     * 状态，初始化0，运行中1，正常结束2, 手动停止 3
     *
     * @see BaseTask#STATE_HAND_STOP
     */
    protected Integer state;

}
