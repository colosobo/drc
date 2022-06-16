package com.timevale.drc.worker.service.sink.rocketmq;

import com.ctrip.framework.apollo.ConfigService;
import com.timevale.drc.base.sinkConfig.RocketSinkConfig;
import com.timevale.drc.worker.service.sink.InternalSinkFactory;
import lombok.extern.slf4j.Slf4j;
import org.h2.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author gwk_2
 * @date 2021/4/20 22:34
 */
@Slf4j
@Component
public class RocketMQSinkFactory {

    private static InternalSinkFactory internalSinkFactory;

    private static final RocketMQSinkFactory ROCKETMQ_SINK_FACTORY = new RocketMQSinkFactory();

    public static RocketMQSinkFactory getInstance() {
        return ROCKETMQ_SINK_FACTORY;
    }

    /**
     * 如此注入比较丑陋, 先这样.
     *
     * @param internalSinkFactory
     */
    @Autowired
    public void autowire(InternalSinkFactory internalSinkFactory) {
        RocketMQSinkFactory.internalSinkFactory = internalSinkFactory;
    }

    public <T> RocketMQSink<T> create(String topic, String nameServer) {
        if (StringUtils.isNullOrEmpty(nameServer)) {
            nameServer = ConfigService.getAppConfig().getProperty("mq.nameSrv.addr", null);
        }
        RocketSinkConfig rocketSinkConfig = new RocketSinkConfig();
        rocketSinkConfig.setTopic(topic);
        rocketSinkConfig.setNameServer(nameServer);

        return (RocketMQSink<T>) internalSinkFactory.create(rocketSinkConfig);
    }
}
