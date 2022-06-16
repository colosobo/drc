package com.timevale.drc.pd.service.param;

import com.timevale.drc.pd.service.vo.DbConfigVO;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.ToString;

/**
 * @author gwk_2
 * @date 2021/5/31 11:01
 */
@Data
@ToString
@ApiModel
public class OpenApiStartParams {

    DbConfigVO master;
    DbConfigVO slave;
    MessageQueueConfig topic;
    String task;
    String tableName;
    Integer qps = 100;


    @Data
    public static class MessageQueueConfig {
        /** kafka,rocketmq */
        String type;
        String servers;
        String topic;
    }


}
