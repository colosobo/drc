package com.timevale.drc.worker.service.canal;

import com.timevale.drc.base.Sink;
import com.timevale.drc.worker.service.sink.rocketmq.RocketMQSink;
import com.timevale.drc.worker.service.sink.rocketmq.RocketMQSinkFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.timevale.drc.base.rocketmq.admin.MQConstants.DRC_MQ_TOPIC_PREFIX;

/**
 * 暂存 Sink, 用于混合同步时, 暂存一些增量数据.
 *
 * 目前是暂存在 rocketmq 中.
 *
 * @author gwk_2
 * @date 2022/4/14 12:56
 */
@Slf4j
public class StagingSink<T> implements Sink<T> {

    private RocketMQSink<T> rocketMQSink;

    private String name;

    public StagingSink(String name) {
        this.name = DRC_MQ_TOPIC_PREFIX + name;
        rocketMQSink = RocketMQSinkFactory.getInstance().create(this.name, System.getProperty("StagingSink.RocketMQ.NameServer"));
    }

    @Override
    public void start() {
        log.info("{} StagingSink start.", this.name);
    }

    @Override
    public void stop() {
        log.info("{} StagingSink stop.", this.name);
    }

    @Override
    public void sink(List<T> t) {
        rocketMQSink.sink(t);
    }

    @Override
    public String getName() {
        return "StagingSink===>RocketMQSink===>" + this.name;
    }
}
