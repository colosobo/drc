package com.timevale.drc.base.sinkConfig;

import lombok.Builder;
import lombok.Data;

/**
 * @author gwk_2
 * @date 2021/4/22 14:07
 */
@Data
@Builder
public class KafkaSinkConfig implements SinkConfig {

    public String kafkaBootstrapServers;
    public String topic;
    public String key;
    /** 单分区开关 */
    public boolean oncePartitionEnabled;
    public int partition;
    public String keySerializer;
    public String valueSerializer;
    public boolean isAsync;
    public int messageFormatType;

    public KafkaSinkConfig() {
    }

    public KafkaSinkConfig(String kafkaBootstrapServers, String topic,
                           String key,
                           boolean oncePartitionEnabled,
                           int partition, String keySerializer,
                           String valueSerializer,
                           boolean isAsync, int messageFormatType) {
        this.kafkaBootstrapServers = kafkaBootstrapServers;
        this.topic = topic;
        this.key = key;
        this.oncePartitionEnabled = oncePartitionEnabled;
        this.partition = partition;
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
        this.isAsync = isAsync;
        this.messageFormatType = messageFormatType;
    }

    @Override
    public int getMessageFormatType() {
        return messageFormatType;
    }

}
