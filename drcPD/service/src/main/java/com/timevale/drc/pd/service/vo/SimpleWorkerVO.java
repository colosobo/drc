package com.timevale.drc.pd.service.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author gwk_2
 * @date 2021/3/17 15:51
 */
@Data
@ApiModel
public class SimpleWorkerVO {

    @ApiModelProperty("task 的详情列表")
    private List<SimpleTask> taskNameList;
    @ApiModelProperty("自身的ipPort")
    private String ipPort;
    @ApiModelProperty("task 的数量")
    private Integer workerCount;
    @ApiModelProperty("自身的ipPort")
    private String name;
    @ApiModelProperty("workerQPS")
    private int workerQPS;

    public SimpleWorkerVO() {
    }

    public SimpleWorkerVO(List<SimpleTask> taskNameList, String ipPort) {
        this.taskNameList = taskNameList;
        this.ipPort = ipPort;
        this.workerCount = taskNameList.size();
        this.name = ipPort + "(Task数量:" + workerCount + ")";
    }

    @Data
    public static class SimpleTask {
        String name;
        int qps;

        public SimpleTask(String name) {
            this.name = name;
        }

        public SimpleTask(String name, int qps) {
            this.qps = qps;
            this.name = name + "(QPS:" + qps + ")";
        }
    }
}
