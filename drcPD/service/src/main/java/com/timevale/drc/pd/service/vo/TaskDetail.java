package com.timevale.drc.pd.service.vo;

import com.timevale.drc.base.TaskStateEnum;
import com.timevale.drc.pd.service.param.OpenApiStartParams;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author gwk_2
 * @date 2021/8/18 20:46
 */
@Data
@ApiModel
public class TaskDetail {

    @ApiModelProperty("主库配置")
    DbConfigVO master;
    @ApiModelProperty("读库配置")
    DbConfigVO slave;
    @ApiModelProperty("topic 配置(servers, topicName, mqType )")
    OpenApiStartParams.MessageQueueConfig topic;
    @ApiModelProperty("任务状态(暂存 STAGING , 回放 PLAYBACK_ING, 直接投递中 RUNNING, 正常结束 OVER, 手动停止 HAND_STOP, 任务异常 EXCEPTION)")
    TaskStateEnum taskStatus;

}
