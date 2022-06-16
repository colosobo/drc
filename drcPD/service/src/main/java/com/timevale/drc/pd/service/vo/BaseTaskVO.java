package com.timevale.drc.pd.service.vo;

import com.timevale.drc.base.BaseTask;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author gwk_2
 * @date 2021/3/23 10:42
 */
@Data
@ApiModel
public class BaseTaskVO {

    public static final int switchState_ON = 1;
    public static final int switchState_OFF = 2;

    private String taskName;

    /**
     * @see BaseTask
     */
    @ApiModelProperty("状态，初始化0，运行中1，正常结束2,  暂停3 , 手动停止 4; (如果是2,开关不显示); 5 db Running;RPC Exception")
    private Integer state;

    @ApiModelProperty("任务类型1增量，2全量，3增量+全量")
    private Integer type;

    @ApiModelProperty("当前的 QPS 配置")
    private Integer qpsConfig;

    @ApiModelProperty("开关状态, 1开启, 2关闭")
    private Integer switchState;

    @ApiModelProperty("如果是运行状态, 这个就是当前的 QPS")
    private Integer currentQPS;
}
