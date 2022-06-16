package com.timevale.drc.worker.service.sink.canal;

import com.alibaba.fastjson.JSON;
import com.alibaba.otter.canal.connector.core.producer.MQDestination;
import com.alibaba.otter.canal.connector.core.spi.CanalMQProducer;
import com.alibaba.otter.canal.connector.core.util.Callback;
import com.alibaba.otter.canal.connector.kafka.producer.CanalKafkaProducer;
import com.alibaba.otter.canal.protocol.Message;
import com.timevale.drc.base.Sink;
import com.timevale.drc.base.sinkConfig.CanalKafkaSinkConfig;
import com.timevale.drc.base.util.GlobalConfigUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Properties;

/**
 * @author gwk_2
 * @date 2021/4/26 14:46
 */
@Slf4j
public class CanalKafkaSink<T> implements Sink<T> {

    private CanalMQProducer canalKafkaProducer;
    private MQDestination canalDestination;

    private final CanalKafkaSinkConfig canalKafkaSinkConfig;
    private final String topicName;

    private final Properties props;
    private boolean started;

    public CanalKafkaSink(CanalKafkaSinkConfig canalKafkaSinkConfig, Properties props) {
        this.canalKafkaSinkConfig = canalKafkaSinkConfig;
        this.topicName = canalKafkaSinkConfig.getTopic();
        this.props = props;
    }

    @Override
    public synchronized void start() {
        if (started) {
            return;
        }
        canalDestination = new MQDestination();
        // 这个 canalDestination 仅仅是 canal 打日志用的.
        canalDestination.setCanalDestination(canalKafkaSinkConfig.getTopic());
        canalDestination.setTopic(canalKafkaSinkConfig.getTopic());
        canalDestination.setPartition(canalKafkaSinkConfig.getPartition());
        canalKafkaProducer = new CanalKafkaProducer();

        canalKafkaProducer.init(this.props);
        started = true;
    }

    @Override
    public void stop() {
    }

    @Override
    public void sink(List<T> list) {
        T t  = list.get(0);
        if (GlobalConfigUtil.getMySQLIncrMsgLogEnabled()) {
            log.info("发送数据到 CanalKafkaSink , topic={}, 内容={} ", topicName, JSON.toJSONString(t));
        }
        if (!started) {
            start();
        }
        if (t instanceof Message) {
            Message message = (Message) t;
            canalKafkaProducer.send(canalDestination, message, new Callback() {

                @Override
                public void commit() {
                    log.debug("canal kafka commit.");
                }

                @Override
                public void rollback() {
                    log.info("canal kafka rollback.");
                }
            });
        } else {
            log.error("消息类型错误, CanalKafkaSink 只接受 com.alibaba.otter.canal.protocol.Message 类型");
            throw new RuntimeException("消息类型错误, CanalKafkaSink 只接受 com.alibaba.otter.canal.protocol.Message 类型");
        }
    }
}
