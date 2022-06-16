package com.timevale.drc.base.sinkConfig;

import lombok.Data;

/**
 * @author gwk_2
 * @date 2021/4/22 14:07
 */
@Data
public class CanalKafkaSinkConfig implements SinkConfig {

    /** 服务器 */
    public String kafkaBootstrapServers;
    /** topic */
    private String topic;
    /** 分区编号 */
    private Integer partition;
    /** 消息格式类型, drc or canal */
    public int messageFormatType;

    public int taskType;

    @Override
    public int getMessageFormatType() {
        return messageFormatType;
    }

}
