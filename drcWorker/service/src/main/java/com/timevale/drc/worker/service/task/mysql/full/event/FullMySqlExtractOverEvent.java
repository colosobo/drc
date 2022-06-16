package com.timevale.drc.worker.service.task.mysql.full.event;

import com.timevale.drc.base.eventbus.Event;
import com.timevale.drc.base.model.DrcSubTaskFullSliceDetail;

/**
 * @author gwk_2
 * @date 2021/3/10 11:07
 */
public class FullMySqlExtractOverEvent implements Event<DrcSubTaskFullSliceDetail> {

    private DrcSubTaskFullSliceDetail drcSubTaskFullSliceDetail;

    public FullMySqlExtractOverEvent(DrcSubTaskFullSliceDetail drcSubTaskFullSliceDetail) {
        this.drcSubTaskFullSliceDetail = drcSubTaskFullSliceDetail;
    }

    @Override
    public DrcSubTaskFullSliceDetail data() {
        return drcSubTaskFullSliceDetail;
    }
}
