package com.timevale.drc.base.alarm;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author gwk_2
 * @date 2020/8/19 21:59
 */
@Builder
@Data
public class DrcAlarmModel {

    /**
     * 消息内容
     */
    private String content;

    /**
     * 花名集合
     */
    private List<String> nickNames;


}
