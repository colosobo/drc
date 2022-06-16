package com.timevale.drc.base.model.result;

import lombok.Data;

@Data
public class SelectIdleWorkerResult {

    String ipPort;

    Integer id;

    Integer count;

}
