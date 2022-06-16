package com.timevale.drc.worker.service.sink.rocketmq;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.timevale.drc.base.Sink;
import com.timevale.drc.base.util.GlobalConfigUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author gwk_2
 * @date 2021/3/8 15:22
 */
@Slf4j
public class RocketMQSink<T> implements Sink<T> {

    private final Gson gson = new Gson();
    private final DefaultMQProducer defaultMQProducer;
    private final String topicName;
    private final String tag;

    public RocketMQSink(DefaultMQProducer producer, String topicName, String tag) {
        this.topicName = topicName;
        this.defaultMQProducer = producer;
        this.tag = tag;
    }

    @Override
    public synchronized void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void sink(List<T> data) {
        if (data == null) {
            return;
        }
        try {
            if (GlobalConfigUtil.getMySQLIncrMsgLogEnabled()) {
                log.info("发送数据到RocketMQ, topic={}, 内容={} ", topicName, JSON.toJSONString(data));
            }

            data.forEach(this::send);
        } catch (Exception e) {
            log.error("MQ sink 失败, msg ={}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void send(T data) {
        if (defaultMQProducer != null) {
            Message message = new Message();

            if (data instanceof String) {
                message.setBody(data.toString().getBytes(StandardCharsets.UTF_8));
            } else {
                message.setBody(gson.toJson(data).getBytes(StandardCharsets.UTF_8));
            }

            message.setTopic(topicName);

            if (tag != null) {
                message.setTags(tag);
            }

            try {
                if (GlobalConfigUtil.mqAsyncEnabled()) {
                    defaultMQProducer.sendOneway(message);
                } else {
                    defaultMQProducer.send(message);
                }
            } catch (Exception e) {
                if (GlobalConfigUtil.ignoreMqExceptionEnabled()) {
                    log.warn(e.getMessage(), e);
                } else {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
