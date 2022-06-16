package com.timevale.drc.pd.service.stat;

import lombok.Data;

@Data
public class ClusterSummary {

    String workerClusterState;
    int clusterTotalQPS;
    int workerCount;
}
