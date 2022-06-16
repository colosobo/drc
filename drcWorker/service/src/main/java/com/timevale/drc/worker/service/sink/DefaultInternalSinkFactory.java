package com.timevale.drc.worker.service.sink;

import com.ctrip.framework.apollo.ConfigService;
import com.timevale.drc.base.Sink;
import com.timevale.drc.base.TaskTypeEnum;
import com.timevale.drc.base.sinkConfig.CanalKafkaSinkConfig;
import com.timevale.drc.base.sinkConfig.KafkaSinkConfig;
import com.timevale.drc.base.sinkConfig.MySQLSinkConfig;
import com.timevale.drc.base.sinkConfig.RocketSinkConfig;
import com.timevale.drc.base.util.JdbcTemplateManager;
import com.timevale.drc.worker.service.sink.canal.CanalKafkaSink;
import com.timevale.drc.worker.service.sink.kafka.KafkaSink;
import com.timevale.drc.worker.service.sink.mysql.MySqlBatchSink;
import com.timevale.drc.worker.service.sink.mysql.MySqlSink;
import com.timevale.drc.worker.service.sink.rocketmq.RocketMQSink;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.kafka.clients.producer.ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION;

/**
 * @author gwk_2
 * @date 2022/3/14 17:31
 */
@Component
public class DefaultInternalSinkFactory implements InternalSinkFactory {

    /** 特殊的告警日志 */
    private static final Logger ALARM_LOG = LoggerFactory.getLogger("alarm");

    private static final String DEFAULT_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";

    private static final Map<String, DefaultMQProducer> ROCKET_MQ_PRODUCER_CACHE = new ConcurrentHashMap<>();

    @Override
    public <T> Sink<T> create(KafkaSinkConfig sinkConfig) {
        Properties props = new Properties();
        //设置接入点，请通过控制台获取对应Topic的接入点
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, sinkConfig.kafkaBootstrapServers);
        //Kafka消息的序列化方式
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, DEFAULT_SERIALIZER);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, DEFAULT_SERIALIZER);
        //请求的最长等待时间
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 30 * 1000);
        // 决定了每次发送给Kafka服务器请求消息的最大大小：1m,但实际上受限于 broker 的吞吐量 (message.max.bytes)
        props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, ConfigService.getAppConfig().getIntProperty("kafka.request.size.max", 1024 * 1024 * 15));
        //设置客户端内部重试次数
        props.put(ProducerConfig.RETRIES_CONFIG, 5);
        //设置客户端内部重试间隔
        props.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, 3000);

        return new KafkaSink<>(
                new KafkaProducer<>(props),
                sinkConfig.topic,
                sinkConfig.key,
                sinkConfig.partition,
                sinkConfig.oncePartitionEnabled,
                sinkConfig.isAsync);
    }

    @Override
    public <T> Sink<T> create(RocketSinkConfig sinkConfig) {
        try {
            DefaultMQProducer producer;
            if (StringUtils.isEmpty(sinkConfig.nameServer)) {
                sinkConfig.nameServer = ConfigService.getAppConfig().getProperty("rocketmq.nameSer.address", null);
            }
            if (StringUtils.isEmpty(sinkConfig.nameServer)) {
                throw new RuntimeException("nameServer isEmpty, config key : rocketmq.nameSer.address");
            }
            if (StringUtils.isEmpty(sinkConfig.topic)) {
                throw new RuntimeException("topic isEmpty");
            }
            // ignore concurrent
            if (ROCKET_MQ_PRODUCER_CACHE.get(sinkConfig.nameServer) != null) {
                producer = ROCKET_MQ_PRODUCER_CACHE.get(sinkConfig.nameServer);
            } else {
                producer = new DefaultMQProducer(sinkConfig.topic + "_GROUP_" + UUID.randomUUID().toString());
                producer.setNamesrvAddr(sinkConfig.nameServer);
                ROCKET_MQ_PRODUCER_CACHE.put(sinkConfig.nameServer, producer);
                producer.start();
            }
            return new RocketMQSink<>(producer, sinkConfig.topic, sinkConfig.tag);
        } catch (Exception e) {
            ALARM_LOG.error("MQProducer启动失败, e:", e);
            throw new RuntimeException("MQProducer 启动失败", e);
        }
    }

    @Override
    public <T> Sink<T> create(MySQLSinkConfig config) {
        boolean batch = config.getTaskTypeEnum() == TaskTypeEnum.MYSQL_FULL_TASK;
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplateManager().get(config.url, config.username, config.pwd, config.database, config.supportLoopSync);
            if (batch) {
                return (Sink<T>) new MySqlBatchSink(jdbcTemplate, config);
            } else {
                return (Sink<T>) new MySqlSink(jdbcTemplate, config);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> Sink<T> create(CanalKafkaSinkConfig sinkConfig) {
        Properties props = new Properties();
        //设置接入点，请通过控制台获取对应Topic的接入点
        props.put("kafka.bootstrap.servers", sinkConfig.getKafkaBootstrapServers());
        //请求的最长等待时间
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 30 * 1000);
        //设置客户端内部重试次数
        props.put(ProducerConfig.RETRIES_CONFIG, 5);
        props.put(MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        //设置客户端内部重试间隔
        props.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, 3000);
        return new CanalKafkaSink<>(sinkConfig, props);
    }
}
