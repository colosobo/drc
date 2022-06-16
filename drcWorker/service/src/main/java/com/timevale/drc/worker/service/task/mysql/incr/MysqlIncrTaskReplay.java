package com.timevale.drc.worker.service.task.mysql.incr;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.timevale.drc.base.rocketmq.admin.MQAdminService;
import com.timevale.drc.base.util.GlobalConfigUtil;
import com.timevale.drc.worker.service.task.mysql.Replay;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.timevale.drc.base.rocketmq.admin.MQConstants.DRC_MQ_REPLAY_CONSUMER_GROUP_PREFIX;
import static com.timevale.drc.base.rocketmq.admin.MQConstants.DRC_MQ_TOPIC_PREFIX;


/**
 * @author gwk_2
 * @date 2021/1/28 23:22
 */
@Slf4j
public class MysqlIncrTaskReplay<M> implements Replay {

    private static final Logger ALARM_LOG = LoggerFactory.getLogger("alarm");

    private final MQAdminService mqAdminService;
    private final MysqlIncrTask<M> task;
    private DefaultMQPushConsumer consumer;
    private String groupName;
    private String topic;

    public MysqlIncrTaskReplay(MQAdminService mqAdminService, MysqlIncrTask<M> task) {
        this.mqAdminService = mqAdminService;
        this.task = task;
    }

    @Override
    public void replay() {
        this.groupName = DRC_MQ_REPLAY_CONSUMER_GROUP_PREFIX + task.getName();
        this.topic = DRC_MQ_TOPIC_PREFIX + task.getName();

        DefaultMQPushConsumer consumer = getConsumer(groupName);
        try {
            consumer.subscribe(topic, "*");
            consumer.registerMessageListener((MessageListenerConcurrently) (m, context) -> consumer(task, topic, m));

            task.getLog().info("增量任务 [" + task.getName() + "] 开启回放consumer ");
            consumer.start();
        } catch (Exception e) {
            ALARM_LOG.error("任务{}回放失败，消费启动失败, e:", task.getName(), e);
        }
    }

    private ConsumeConcurrentlyStatus consumer(MysqlIncrTask<M> task, String topic, List<MessageExt> message) {
        for (MessageExt msg : message) {
            String body = new String(msg.getBody(), Charsets.UTF_8);

            if (GlobalConfigUtil.getMySQLIncrMsgLogEnabled()) {
                task.getLog().info("消费RocketMQ消息, topic=" + topic + ", 内容=" + body);
            }

            task.rRateLimiter.acquire();

            M m = task.getMessageHandler().convFromJson(body);

            task.getTaskMetrics().stat();

            FailRetryUtil.failRetry(task, () -> task.directSink.sink(Lists.newArrayList(m)), e -> task.getLog().info("回放失败,正在重试...."));
        }

        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    @Override
    public void stop() {
        if (consumer != null) {
            consumer.shutdown();
        }
    }

    @Override
    public long getDiff() {
        return mqAdminService.getDiffTotal(groupName);
    }

    private DefaultMQPushConsumer getConsumer(String group) {
        if (consumer == null) {
            consumer = new DefaultMQPushConsumer(group);
            consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
            // 防止乱序消费.
            consumer.setConsumeThreadMin(1);
            consumer.setConsumeThreadMax(1);
        }
        return consumer;
    }
}
