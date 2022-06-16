package com.timevale.drc.pd.service;

import lombok.Getter;

@Getter
public enum WorkerClusterState {

    HEALTH(1, "集群正常"),
    SELF_RECOVERY(2, "故障自愈中..."),
    UN_KNOW(3, "未知状态"),
    RE_BALANCE(4, "RE BALANCE..."),
    FAILOVER(5, "failover..."),
    ;


    int code;
    String desc;

    WorkerClusterState(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
