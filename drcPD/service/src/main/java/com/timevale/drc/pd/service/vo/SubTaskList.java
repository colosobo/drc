package com.timevale.drc.pd.service.vo;

import lombok.Data;

import java.util.List;

@Data
public class SubTaskList {

    List<BaseTaskVO> list;
    int totalQPS;
}
