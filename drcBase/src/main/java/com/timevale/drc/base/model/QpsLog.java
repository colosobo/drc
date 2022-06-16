package com.timevale.drc.base.model;

import lombok.Data;

import javax.persistence.Table;

/**
 * @author gwk_2
 * @date 2021/4/16 13:56
 */
@Data
@Table(name = "drc_qps_log")
public class QpsLog extends BaseDO {

    String name;

    Integer qps;

    Long timeInSeconds;
}
