package com.timevale.drc.worker.service.task.mysql;

import com.timevale.drc.base.Sink;
import com.timevale.drc.base.SinkFactory;
import com.timevale.drc.base.TaskTypeEnum;
import com.timevale.drc.base.binlog.Binlog2JsonModel;
import com.timevale.drc.base.dao.*;
import com.timevale.drc.base.model.*;
import com.timevale.drc.base.model.bo.DrcSubTaskIncrBO;
import com.timevale.drc.base.rocketmq.admin.MQAdminService;
import com.timevale.drc.base.serialize.GenericJackson2JsonSerializer;
import com.timevale.drc.base.sinkConfig.KafkaSinkConfig;
import com.timevale.drc.base.sinkConfig.SinkConfig;
import com.timevale.drc.base.util.DrcZkClient;
import com.timevale.drc.base.util.JdbcTemplateManager;
import com.timevale.drc.base.util.PropertiesUtil;
import com.timevale.drc.worker.service.WorkerServer;
import com.timevale.drc.worker.service.canal.support.DrcLogAlarmHandler;
import com.timevale.drc.worker.service.exp.NotFoundTaskException;
import com.timevale.drc.worker.service.task.Coordinator;
import com.timevale.drc.worker.service.task.mysql.full.MysqlFullTask;
import com.timevale.drc.worker.service.task.mysql.incr.MysqlIncrTask;
import com.timevale.drc.worker.service.task.mysql.incr.support.BaseIncrTaskFactory;
import com.timevale.drc.worker.service.task.mysql.incr.support.Binlog2JsonModelIncrTaskFactory;
import com.timevale.drc.worker.service.task.mysql.incr.support.CanalRowIncrTaskFactory;
import com.timevale.drc.worker.service.task.mysql.incr.support.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.timevale.drc.base.util.PropertiesKey.*;

/**
 * @author gwk_2
 * @date 2021/3/8 15:48
 */
@Component
@Slf4j
public class DefaultTaskFactory implements TaskFactory {

    private final static GenericJackson2JsonSerializer SERIALIZER = new GenericJackson2JsonSerializer();

    @Value("${MySqlFullExtract.limit:100}")
    private Integer limit;
    @Value("${drc.zk.addr}")
    private String drcZkAddr;
    @Value("${kafka.bootstrap.servers:}")
    private String kafkaBootstrapServers;


    @Resource
    private DrcSubTaskFullConfigMapper fullConfigMapper;
    @Resource
    private DrcDbConfigMapper drcDbConfigMapper;
    @Resource
    private DrcSubTaskFullSliceDetailMapper drcSubTaskFullSliceDetailMapper;
    @Resource
    private DrcSubTaskIncrMapper drcSubTaskIncrMapper;
    @Resource(name = "RedisCoordinator")
    private Coordinator coordinator;
    @Resource
    private MQAdminService mqAdminService;
    @Resource
    private DrcZkClient drcZkClient;
    @Resource
    private DrcTaskMapper drcTaskMapper;
    @Resource
    private DrcTaskRegisterTableMapper drcTaskRegisterTableMapper;
    @Resource
    private WorkerServer workerServer;
    @Resource
    private DrcDbConfigMapper dbConfigMapper;
    @Resource
    private RedissonClient redisson;
    @Resource
    private SinkFactory sinkFactory;
    @Resource
    private DrcLogAlarmHandler drcLogAlarmHandler;

    private final JdbcTemplateManager jdbcTemplateManager = new JdbcTemplateManager();
    private final Map<MessageHandler.MessageType, BaseIncrTaskFactory<?>> incrFactoryMap = new HashMap<>();


    @PostConstruct
    public void init() {
        BaseIncrTaskFactory<?> drcFactory = new Binlog2JsonModelIncrTaskFactory(workerServer,
                drcSubTaskIncrMapper,
                drcDbConfigMapper,
                drcZkClient,
                mqAdminService,
                drcTaskRegisterTableMapper,
                sinkFactory);

        BaseIncrTaskFactory<?> canalFactory = new CanalRowIncrTaskFactory(workerServer,
                drcSubTaskIncrMapper,
                drcDbConfigMapper,
                drcZkClient,
                mqAdminService,
                drcTaskRegisterTableMapper,
                sinkFactory);

        incrFactoryMap.put(MessageHandler.MessageType.DRC, drcFactory);
        incrFactoryMap.put(MessageHandler.MessageType.CANAL, canalFactory);
    }


    @Override
    public MysqlFullTask createFullTask(DrcSubTaskFullSliceDetail subTask) {

        Integer parentId = subTask.getParentId();
        DrcTask drcTask = drcTaskMapper.selectByPrimaryKey(parentId);
        if (drcTask == null) {
            throw new RuntimeException("parent task is null....SubTaskName={}" + subTask.getSubTaskName());
        }

        Integer drcSubTaskFullConfigId = subTask.getDrcSubTaskFullConfigId();
        DrcSubTaskFullConfig config = fullConfigMapper.selectByPrimaryKey(drcSubTaskFullConfigId);
        String sinkJson = config.getSinkJson();

        SinkConfig sinkConfig;
        if (StringUtils.isBlank(sinkJson)) {
            // 默认
            sinkConfig = getDefault(drcTask, subTask.getSubTaskName(), false);
        } else {
            sinkConfig = SERIALIZER.deSerializer(sinkJson);
        }

        Sink<Binlog2JsonModel> sink = sinkFactory.create(sinkConfig.taskType(TaskTypeEnum.MYSQL_FULL_TASK));

        // 分布式限流.
        RRateLimiter rateLimiter = redisson.getRateLimiter(drcTask.getTaskName());

        return new MysqlFullTask(
                fullConfigMapper,
                drcDbConfigMapper,
                drcSubTaskFullSliceDetailMapper,
                jdbcTemplateManager,
                subTask,
                limit,
                sink,
                rateLimiter,
                drcTaskMapper);
    }

    @Override
    public MysqlIncrTask<?> createIncrTask(DrcSubTaskIncrBO drcSubTaskIncrBO) {
        Properties properties = getProperties(drcSubTaskIncrBO);
        Integer parentId = drcSubTaskIncrBO.getParentId();
        DrcTask parentTask = drcTaskMapper.selectByPrimaryKey(parentId);
        if (parentTask == null) {
            throw new NotFoundTaskException("parentId 错误, parentId=" +
                    parentId + ", drcSubTaskIncr = " + drcSubTaskIncrBO.getSubTaskName());
        }

        String sinkJson = drcSubTaskIncrBO.getSinkJson();
        if (StringUtils.isEmpty(sinkJson)) {
            // 兼容老的设计.
            sinkJson = parentTask.getSinkJson();
        }

        SinkConfig sinkConfig;
        int messageFormat = MessageHandler.MessageType.DRC.getCode();

        RRateLimiter rateLimiter = redisson.getRateLimiter(parentTask.getTaskName());

        if (StringUtils.isBlank(sinkJson)) {
            sinkConfig = getDefault(parentTask, drcSubTaskIncrBO.getSubTaskName(), true);
        } else {
            sinkConfig = SERIALIZER.deSerializer(sinkJson);
            if (sinkConfig instanceof KafkaSinkConfig) {
                KafkaSinkConfig kafkaSinkConfig = (KafkaSinkConfig) sinkConfig;
                // 增量场景下, 必须开启单分区.
                kafkaSinkConfig.setOncePartitionEnabled(true);
            }
            messageFormat = sinkConfig.getMessageFormatType();
        }
        sinkConfig = sinkConfig.taskType(TaskTypeEnum.MYSQL_INCR_TASK);

        MessageHandler.MessageType type = MessageHandler.MessageType.conv(messageFormat);

        return incrFactoryMap.get(type)
                .create(sinkConfig,
                        drcZkAddr,
                        parentTask,
                        coordinator,
                        properties,
                        drcSubTaskIncrBO,
                        rateLimiter,
                        drcTaskMapper,
                        drcLogAlarmHandler);
    }

    private SinkConfig getDefault(DrcTask parentTask, String subTaskName, boolean oncePartitionEnabled) {
        SinkConfig sinkConfig;
        sinkConfig = KafkaSinkConfig.builder()
                .kafkaBootstrapServers(kafkaBootstrapServers)
                .topic(parentTask.getTaskName())
                .key(subTaskName)
                .oncePartitionEnabled(oncePartitionEnabled)
                .build();
        return sinkConfig;
    }

    private Properties getProperties(DrcSubTaskIncr drcSubTaskIncr) {
        Properties properties;
        try {
            DrcDbConfig drcDbConfig = dbConfigMapper.selectByPrimaryKey(drcSubTaskIncr.getDbConfigId());

            properties = PropertiesUtil.load("instance-template.properties");
            properties.put(canal_instance_master_address, drcDbConfig.getUrl());
            properties.put(canal_instance_dbUsername, drcDbConfig.getUsername());
            properties.put(canal_instance_dbPassword, drcDbConfig.getPassword());
            if (StringUtils.isBlank(drcSubTaskIncr.getTableExpression())) {
                properties.put(canal_instance_filter_regex, drcDbConfig.getDatabaseName() + "\\..*");
            } else {
                properties.put(canal_instance_filter_regex, drcSubTaskIncr.getTableExpression());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return properties;
    }
}
