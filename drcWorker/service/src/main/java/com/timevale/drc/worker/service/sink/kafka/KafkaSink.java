package com.timevale.drc.worker.service.sink.kafka;

import com.google.gson.Gson;
import com.timevale.drc.base.Sink;
import com.timevale.drc.base.alarm.AlarmUtil;
import com.timevale.drc.base.util.GlobalConfigUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.RecordTooLargeException;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author gwk_2
 * @date 2021/3/8 15:22
 */
@Slf4j
public class KafkaSink<T> implements Sink<T> {

    private final Gson gson = new Gson();

    private final KafkaProducer<String, String> producer;
    private final String topic;
    private final String key;
    /**
     * 固定 partition,  保证绝对有序. 如果是全量同步, 就不需要这么弄了
     */
    private final int partition;
    private final boolean oncePartitionEnabled;
    private final Callback callback = new MyCallback();
    private final boolean isAsync;

    public KafkaSink(KafkaProducer<String, String> producer, String topic, String key, int partition, boolean oncePartitionEnabled, boolean isAsync) {
        this.producer = producer;
        this.topic = topic;
        this.key = key;
        this.partition = partition;
        this.oncePartitionEnabled = oncePartitionEnabled;
        this.isAsync = isAsync;
    }

    @Override
    public void start() {
        log.info("{} KafkaSink start.", this.topic);
    }

    @Override
    public void stop() {
        producer.close();
    }

    @Override
    public void sink(List<T> t) {
        String value = gson.toJson(t.get(0));
        if (GlobalConfigUtil.getMySQLIncrMsgLogEnabled()) {
            log.info("发送数据到kafka, key={}, topic={}, 内容={} ", key, topic, value);
        }
        ProducerRecord<String, String> record = buildRecord(value);

        try {
            if (isAsync || GlobalConfigUtil.kafkaAsyncEnable()) {
                producer.send(record, callback);
            } else {
                producer.send(record).get();
            }
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RecordTooLargeException) {
                if (value.length() > 10240) {
                    log.error("发送 kafka 异常, 发送topic={}, 内容超过了10240长度 ", topic);
                } else {
                    log.error("发送 kafka 异常, 发送topic={}, 内容={} ", topic, value);
                }
                AlarmUtil.pushAlarm2Admin("发送 kafka 数据太大异常, 已忽略并打印日志, 发送topic = " + topic);
                return;
            } else {
                throw new RuntimeException(e.getMessage(), e);
            }
        } catch (Exception e) {
            if (value.length() > 10240) {
                log.error("发送 kafka 异常, 发送topic={}, 内容超过了10240长度 ", topic);
            } else {
                log.error("发送 kafka 异常, 发送topic={}, 内容={} ", topic, value);
            }
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private ProducerRecord<String, String> buildRecord(String value) {
        ProducerRecord<String, String> record;
        if (oncePartitionEnabled) {
            record = new ProducerRecord<>(topic, partition, key, value);
        } else {
            record = new ProducerRecord<>(topic, key, value);
        }
        return record;
    }

    class MyCallback implements Callback {

        @Override
        public void onCompletion(RecordMetadata metadata, Exception e) {
            if (e != null) {
                log.error("异步 kafka 发送失败... topic = {} " + e.getMessage(), topic, e);
            }
        }
    }
}
