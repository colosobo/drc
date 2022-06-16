package com.timevale.drc.base.model.bo;

import com.timevale.drc.base.model.DrcSubTaskIncr;
import com.timevale.drc.base.model.ext.DrcSubTaskIncrExt;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DrcSubTaskIncrBO extends DrcSubTaskIncr {

    private DrcSubTaskIncrExt drcSubTaskIncrExt;
}
