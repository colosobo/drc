package com.timevale.drc.base.sinkConfig;

import lombok.Data;

/**
 * @author gwk_2
 * @date 2021/4/22 14:12
 */
@Data
public class RocketSinkConfig implements SinkConfig {

    public String nameServer;
    public String topic;
    public String tag;
    public int messageFormatType;

    @Override
    public int getMessageFormatType() {
        return messageFormatType;
    }

}
