package com.timevale.drc.pd.service.vo;


import com.timevale.drc.base.BaseTask;
import com.timevale.drc.base.sinkConfig.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("添加任务的基础对象")
public class DrcTaskVO {

    @ApiModelProperty("任务名称")
    private String taskName;

    @ApiModelProperty("任务描述")
    private String desc;

    /**
     * 类型，
     * @see BaseTask#TYPE_INCR_TASK
     * @see BaseTask#TYPE_FULL_TASK
     * @see BaseTask#TYPE_MIX_TASK
     */
    @ApiModelProperty("类型, 1增量,2全量, 3混合")
    private Integer type;

    @ApiModelProperty("qps 的限流配置")
    private Integer qpsLimitConfig;

    /**
     * @see SinkConfig.Type
     */
    @ApiModelProperty("0没有, 1kafka,  2mysql, 3rocketmq, 4CanalKafka")
    private int sinkType;

    private KafkaSinkConfig kafkaSinkConfig;

    private MySQLSinkConfig mySQLSinkConfig;

    private RocketSinkConfig rocketSinkConfig;

    private CanalKafkaSinkConfig canalKafkaSinkConfig;

    private String sinkJson;
}
