package com.timevale.drc.base.model;

import lombok.Data;

import javax.persistence.Table;

@Data
@Table(name = "drc_task_register_table")
public class DrcTaskRegisterTable extends BaseDO {

    private Integer workerId;

    private String workerIpPort;

    private String taskName;

    private String ext;
}
