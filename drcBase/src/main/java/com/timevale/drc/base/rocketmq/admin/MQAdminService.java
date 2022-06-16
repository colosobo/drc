package com.timevale.drc.base.rocketmq.admin;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.timevale.drc.base.serialize.JackSonUtil;
import com.timevale.drc.base.util.GlobalConfigUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.admin.ConsumeStats;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.subscription.SubscriptionGroupConfig;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

import static org.apache.rocketmq.common.MixAll.NAMESRV_ADDR_PROPERTY;

@Getter
@Setter
@Slf4j
@Service
public class MQAdminService extends MQAdminExtImpl {

    private static final Logger ALARM_LOG = LoggerFactory.getLogger("alarm");

    @Value("${mq.nameSrv.addr:}")
    public String nameSrvAddr;

    static List<String> addrList;

    private static MQAdminService mqAdminService;

    @PostConstruct
    public void initAddrList() {
        String[] arr = nameSrvAddr.split(";");
        addrList = new ArrayList<>();
        for (String s : arr) {
            addrList.add(s);
        }
        System.getProperties().put(NAMESRV_ADDR_PROPERTY, nameSrvAddr);
        mqAdminService = this;
    }

    public String createMQTopicAndConsumerGroup(String name) {
        try {
            log.info("增量任务{}，创建MQ Topic/ConsumerGroup", name);
            createMQTopic(name);
            String mqConsumerGroup = createMQConsumerGroup(name);
            log.info("增量任务{}，创建MQ Topic/ConsumerGroup成功", name);
            return mqConsumerGroup;
        } catch (Exception e) {
            ALARM_LOG.error("创建MQ Topic/ConsumerGroup失败，task:{}, ex:", name, e);
            throw new RuntimeException("创建MQ Topic/ConsumerGroup失败");
        }
    }

    public void deleteMQTopicAndConsumerGroup(String name) {
        if(StringUtils.isEmpty(nameSrvAddr)){
            return;
        }

        try {
            log.info("增量任务{}，删除MQ Topic/ConsumerGroup", name);
            deleteMQTopic(name);
            deleteMQConsumerGroup(name);
            log.info("增量任务{}，删除MQ Topic/ConsumerGroup成功", name);
        } catch (Exception e) {
            ALARM_LOG.error("删除MQ Topic/ConsumerGroup失败，task:{}, ex:", name, e);
            throw new RuntimeException("删除MQ Topic/ConsumerGroup失败");
        }
    }

    private void createMQTopic(String topicName) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        TopicConfig config = new TopicConfig();
        config.setWriteQueueNums(1);
        config.setReadQueueNums(1);
        config.setOrder(true);
        config.setTopicName(MQConstants.DRC_MQ_TOPIC_PREFIX + topicName);
        createAndUpdateTopicConfig(config);
    }

    public long getDiffTotal(String consumerGroup) {
        ConsumeStats consumeStats = null;
        try {
            consumeStats = MQAdminInstance.threadLocalMQAdminExt().examineConsumeStats(consumerGroup);
        } catch (Exception e) {
            log.warn("examineConsumeStats exception, " + consumerGroup, e);
        }

        if (consumeStats != null) {
            return consumeStats.computeTotalDiff();
        }
        return -1;

    }

    public String createMQConsumerGroup(String name) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        String groupName = MQConstants.DRC_MQ_REPLAY_CONSUMER_GROUP_PREFIX + name;
        for (Map.Entry<String, List<String>> entry : brokerList(true).entrySet()) {
            SubscriptionGroupConfig config = new SubscriptionGroupConfig();
            config.setGroupName(groupName);
            createAndUpdateSubscriptionGroupConfig(entry.getValue().get(0), config);
        }
        return groupName;
    }

    private void deleteMQTopic(String name) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        if(StringUtils.isEmpty(nameSrvAddr)){
            return;
        }

        Set<String> brokerSet = new HashSet<>();
        Map<String, List<String>> brokerList = brokerList(true);
        for (List<String> value : brokerList.values()) {
            brokerSet.addAll(value);
        }
        deleteTopic(brokerSet, Collections.singleton(nameSrvAddr), MQConstants.DRC_MQ_TOPIC_PREFIX + name);
    }

    private void deleteMQConsumerGroup(String name) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        if(StringUtils.isEmpty(nameSrvAddr)){
            return;
        }

        String groupName = MQConstants.DRC_MQ_REPLAY_CONSUMER_GROUP_PREFIX + name;
        for (Map.Entry<String, List<String>> entry : brokerList(true).entrySet()) {
            deleteSubscriptionGroup(entry.getValue().get(0), groupName);
        }
    }

    public void createAndUpdateTopicConfig(TopicConfig config)
            throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        createAndUpdateTopicConfig(getRandomMasterBrokerAddr(), config);
    }

    public String getRandomMasterBrokerAddr() {
        Map<String, List<String>> stringListMap = brokerList(true);
        List<String> allAddrList = Lists.newArrayList();
        stringListMap.values().forEach(allAddrList::addAll);
        Collections.shuffle(allAddrList);
        return allAddrList.get(0);
    }

    /**
     * 获取 broker 集合
     *
     * @return
     */
    public Map<String, List<String>> brokerList(boolean master) {
        try {
            ClusterInfo clusterInfo = examineBrokerClusterInfo();

            Map<String, List<String>> brokerDatas = new HashMap<>();

            List<String> brokerList = GlobalConfigUtil.getBrokerList();

            for (Map.Entry<String, BrokerData> entry : clusterInfo.getBrokerAddrTable().entrySet()) {
                String brokerName = entry.getKey();

                // 只使用这两个 broker.
                if (!brokerList.contains(brokerName)) {
                    continue;
                }
                BrokerData brokerData = entry.getValue();
                List<String> addrs = new ArrayList<>();
                if (master) {
                    addrs.add(new ArrayList<>(brokerData.getBrokerAddrs().values()).get(0));
                } else {
                    addrs.addAll(brokerData.getBrokerAddrs().values());
                }

                brokerDatas.putIfAbsent(brokerName, addrs);
            }
            return brokerDatas;
        } catch (Exception err) {
            throw Throwables.propagate(err);
        }
    }

    /**
     * 创建并更新 topic 配置
     **/
    @Override
    public void createAndUpdateTopicConfig(String addr, TopicConfig config)
            throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().createAndUpdateTopicConfig(addr, config);
    }

    /**
     * 创建并更新 订阅组 配置.
     */
    @Override
    public void createAndUpdateSubscriptionGroupConfig(String addr, SubscriptionGroupConfig config)
            throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().createAndUpdateSubscriptionGroupConfig(addr, config);
    }

    /**
     * 删除 订阅组 配置.
     */
    @Override
    public void deleteSubscriptionGroup(String addr, String groupName)
            throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().deleteSubscriptionGroup(addr, groupName);
    }

    /**
     * 删除 topic
     */
    public void deleteTopic(Set<String> brokerAddrs, Set<String> nameSrvAddrs, String topic)
            throws InterruptedException, RemotingException, MQClientException, MQBrokerException {

        deleteTopicInBroker(brokerAddrs, topic);
        deleteTopicInNameServer(nameSrvAddrs, topic);
    }

    /**
     * 删除 topic. in broker.
     */
    @Override
    public void deleteTopicInBroker(Set<String> addrs, String topic)
            throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        log.info("addrs={} topic={}", JackSonUtil.obj2String(addrs), topic);
        MQAdminInstance.threadLocalMQAdminExt().deleteTopicInBroker(addrs, topic);
    }

    /**
     * 删除 topic. in nameServer.
     */
    @Override
    public void deleteTopicInNameServer(Set<String> addrs, String topic)
            throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().deleteTopicInNameServer(addrs, topic);
    }

    public List<String> getAddrList() {
        return addrList;
    }

}
