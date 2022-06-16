package com.timevale.drc.pd.service.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author gwk_2
 * @date 2021/3/17 18:00
 */
@ApiModel
@Data
public class ParentTaskVO {

    private Integer id;

    private String name;

    @ApiModelProperty("增量1, 全量2, 混合3")
    private Integer type;

    private String desc;

    @ApiModelProperty("是否可以拆分, false 不需要. 拆分前, 需要启动增量 task(如果 type 是混合类型).")
    private Boolean canSplit = false;

    private String createTime;

    private Integer state;

    @ApiModelProperty("kafka 的消息总数")
    private Long kafkaTotal;

    @ApiModelProperty("kafka 的消息最后更新时间")
    private String kafkaUpdateTime;

    private int qps;

    @ApiModelProperty("含有异常任务")
    private boolean hasException;

    @ApiModelProperty("含有正常任务")
    private boolean hasNormal;

    @ApiModelProperty("Sink 目标")
    private String destName;
}
