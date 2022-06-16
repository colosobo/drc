package com.timevale.drc.base.metrics;

import lombok.Data;

/**
 * @author gwk_2
 * @date 2022/3/8 10:45
 */
@Data
public class TaskMetricsModel {

    private int qps;
    private long timestamp;
    private String name;
}
