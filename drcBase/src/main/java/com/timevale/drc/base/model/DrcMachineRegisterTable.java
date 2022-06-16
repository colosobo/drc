package com.timevale.drc.base.model;

import lombok.Data;

import javax.persistence.Table;

@Data
@Table(name = "drc_machine_register_table")
public class DrcMachineRegisterTable extends BaseDO {

    public static final int TYPE_PD = 1;
    public static final int TYPE_WORKER = 2;

    /**
     * ip:port
     */
    private String ipPort;

    /**
     * 1 pd
     * 2 worker
     */
    private Integer type;

    private String extInfo;

}
