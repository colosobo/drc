package com.timevale.drc.pd.service;

import com.aliyun.alikafka20190916.models.CreateTopicRequest;
import com.aliyun.alikafka20190916.models.CreateTopicResponse;
import com.aliyun.alikafka20190916.models.DeleteTopicRequest;
import com.aliyun.alikafka20190916.models.DeleteTopicResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.timevale.drc.pd.service.kms.KmsComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author gwk_2
 * @date 2021/3/24 14:19
 */
@Component
@Slf4j
public class AliYunKafkaOpenApiClient {

    public static final String ALI_HANG_ZHOU_END_POINT = "alikafka.cn-hangzhou.aliyuncs.com";

    @Value("${aliyun.kafka.instanceId:}")
    private String instanceId;

    @Value("${aliyun.kafka.regionId:cn-hangzhou}")
    private String regionId;

    @Value("${aliyun.accessKeyId:null}")
    private String accessKeyId;

    @Value("${aliyun.accessKeySecret:null}")
    private String accessKeySecret;

    private com.aliyun.alikafka20190916.Client client;

    @Resource
    private KmsComponent kmsComponent;

    public void createTopic(String topic) {
        try {
            if (topic.length() > 64) {
                throw new RuntimeException("长度超过 64 个字符.");
            }
            CreateTopicRequest createTopicRequest = new CreateTopicRequest()
                    .setInstanceId(instanceId)
                    .setTopic(topic)
                    // 6 的倍数.
                    .setPartitionNum("6")
                    .setRemark("_remark")
                    .setRegionId(regionId);
            CreateTopicResponse res = getClient().createTopic(createTopicRequest);
            if (!res.getBody().getSuccess()) {
                throw new RuntimeException(res.getBody().getMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteTopic(String topic) {
        try {
            DeleteTopicRequest deleteTopicRequest = new DeleteTopicRequest()
                    .setInstanceId(instanceId)
                    .setTopic(topic)
                    .setRegionId(regionId);
            DeleteTopicResponse res = getClient().deleteTopic(deleteTopicRequest);
            if (!res.getBody().getSuccess()) {
                throw new RuntimeException(res.getBody().getMessage());
            }
        } catch (TeaException e) {
            if (e.getCode().equals("BIZ_TOPIC_NOT_FOUND")) {
                // 可能是么有,导致删除失败, 就不
                log.warn(e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized com.aliyun.alikafka20190916.Client getClient() {
        try {
            if (client == null) {
                Config config = new Config()
                        .setAccessKeyId(kmsComponent.kmsDecrypt(accessKeyId))
                        .setAccessKeySecret(kmsComponent.kmsDecrypt(accessKeySecret));
                config.endpoint = ALI_HANG_ZHOU_END_POINT;
                client = new com.aliyun.alikafka20190916.Client(config);
            }
            return client;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
