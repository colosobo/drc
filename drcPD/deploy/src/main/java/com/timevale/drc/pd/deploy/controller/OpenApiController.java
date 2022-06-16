package com.timevale.drc.pd.deploy.controller;

import com.google.gson.Gson;
import com.timevale.drc.base.Task;
import com.timevale.drc.base.TaskStateEnum;
import com.timevale.drc.base.TaskTypeEnum;
import com.timevale.drc.base.dao.DrcTaskMapper;
import com.timevale.drc.base.model.DrcTask;
import com.timevale.drc.base.serialize.GenericJackson2JsonSerializer;
import com.timevale.drc.base.sinkConfig.KafkaSinkConfig;
import com.timevale.drc.base.sinkConfig.RocketSinkConfig;
import com.timevale.drc.base.sinkConfig.SinkConfig;
import com.timevale.drc.base.util.TaskNameBuilder;
import com.timevale.drc.pd.service.PDTaskService;
import com.timevale.drc.pd.service.param.OpenApiStartParams;
import com.timevale.drc.pd.service.param.OpenApiStopParams;
import com.timevale.drc.pd.service.param.OpenApiTaskDetailParams;
import com.timevale.drc.pd.service.task.TaskManager;
import com.timevale.drc.pd.service.vo.*;
import com.timevale.drc.pd.service.vo.universal.MyQueryResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author gwk_2
 * @date 2021/5/31 10:54
 */
@Slf4j
@RestController
@RequestMapping("/openApi")
@Api("DRC 开放接口文档")
public class OpenApiController {
    private final GenericJackson2JsonSerializer serializer = new GenericJackson2JsonSerializer();
    @Autowired
    private TaskController taskController;
    @Autowired
    private DrcTaskMapper drcTaskMapper;
    @Autowired
    private TaskManager taskManager;
    @Autowired
    private PDTaskService pdTaskService;

    @ApiOperation("创建并启动任务")
    @PostMapping("/task/start")
    public MyQueryResult<String> start(@RequestBody OpenApiStartParams openApiStartParams) throws InterruptedException {
        log.info("创建并启动任务, 配置 : {}", new Gson().toJson(openApiStartParams));
        taskController.addMixTask(convert(openApiStartParams));
        DrcTask drcTask = drcTaskMapper.selectByName(openApiStartParams.getTask());
        try {
            taskController.operateParentTask(drcTask.getId(), 1);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            delete(new OpenApiStopParams(openApiStartParams.getTask()));
            return new MyQueryResult<>(false, "启动失败, 原因: " + e.getMessage());
        }

        return new MyQueryResult<>(true, "启动成功");
    }

    @ApiOperation("停止任务")
    @PostMapping("/task/stop")
    public MyQueryResult<String> stop(@RequestBody OpenApiStopParams openApiStopParams) throws InterruptedException {
        DrcTask drcTask = drcTaskMapper.selectByName(openApiStopParams.getTask());
        if (drcTask == null) {
            throw new RuntimeException("task 名称错误." + openApiStopParams.getTask());
        }
        taskController.operateParentTask(drcTask.getId(), 0);
        return new MyQueryResult<>(true, "停止成功");
    }

    @ApiOperation("删除任务")
    @PostMapping("/task/delete")
    public MyQueryResult<String> delete(@RequestBody OpenApiStopParams openApiStopParams) throws InterruptedException {
        DrcTask drcTask = drcTaskMapper.selectByName(openApiStopParams.getTask());
        if (drcTask == null) {
            throw new RuntimeException("task 名称错误." + openApiStopParams.getTask());
        }
        taskController.deleteParentTask(drcTask.getId());
        return new MyQueryResult<>(true, "删除成功");
    }

    @ApiOperation("查看进度")
    @PostMapping("/task/stage")
    public MyQueryResult<TaskStateEnum> stage(@RequestBody OpenApiStopParams openApiStopParams) throws InterruptedException {
        DrcTask drcTask = drcTaskMapper.selectByName(openApiStopParams.getTask());
        if (drcTask == null) {
            throw new RuntimeException("task 名称错误." + openApiStopParams.getTask());
        }
        Task task = taskManager.getTaskWithOutCreate(TaskNameBuilder.buildIncrName(drcTask.getTaskName()));

        if (task == null) {
            log.info("task 找不到 {}", openApiStopParams.getTask());
            return new MyQueryResult<TaskStateEnum>(false, "task 找不到").setResultObject(null);
        }

        TaskStateEnum state = task.getState();
        return new MyQueryResult<TaskStateEnum>(true, "查询成功").setResultObject(state);
    }

    @ApiOperation("查看任务详情")
    @PostMapping("/task/detail")
    public MyQueryResult<TaskDetail> detail(@RequestBody OpenApiTaskDetailParams openApiStopParams) throws InterruptedException {
        DrcTask drcTask = drcTaskMapper.selectByName(openApiStopParams.getTask());
        if (drcTask == null) {
            throw new RuntimeException("task 名称错误." + openApiStopParams.getTask());
        }
        Task task = taskManager.getTaskWithOutCreate(TaskNameBuilder.buildIncrName(drcTask.getTaskName()));

        if (task == null) {
            log.info("task 找不到 {}", openApiStopParams.getTask());
            return new MyQueryResult<TaskDetail>(false, "task 找不到").setResultObject(null);
        }

        TaskDetail state = pdTaskService.getTaskDetail(openApiStopParams.getTask());
        return new MyQueryResult<TaskDetail>(true, "查询成功").setResultObject(state);
    }


    MixTaskInput convert(OpenApiStartParams openApiStartParams) {
        MixTaskInput mixTaskInput = new MixTaskInput();
        mixTaskInput.setFullDbConfig(openApiStartParams.getSlave());
        mixTaskInput.setIncrDbConfig(openApiStartParams.getMaster());

        mixTaskInput.setDrcTaskVO(buildDrcTaskVO(openApiStartParams));
        mixTaskInput.setDrcSubTaskIncrVO(new DrcSubTaskIncrVO(openApiStartParams.getMaster().getDatabase() + "." + openApiStartParams.getTableName()));
        mixTaskInput.setFullTaskConfigVO(new FullTaskConfigVO(200, openApiStartParams.getTableName()));
        return mixTaskInput;
    }

    private DrcTaskVO buildDrcTaskVO(OpenApiStartParams openApiStartParams) {
        // 自家调用,少点防御性代码.
        DrcTaskVO drcTaskVO = new DrcTaskVO();
        drcTaskVO.setTaskName(openApiStartParams.getTask());
        drcTaskVO.setDesc("thanos 任务");
        drcTaskVO.setType(TaskTypeEnum.MYSQL_MIX_TASK.code);
        drcTaskVO.setQpsLimitConfig(openApiStartParams.getQps());

        if (StringUtils.isEmpty(openApiStartParams.getTopic().getType())) {
            throw new RuntimeException("topic type 不能是空.");
        }

        SinkConfig sinkConfig = null;

        if ("kafka".equalsIgnoreCase(openApiStartParams.getTopic().getType())) {
            drcTaskVO.setSinkType(SinkConfig.Type.KAFKA.getCode());
            KafkaSinkConfig kafkaSinkConfig = new KafkaSinkConfig();
            String topic = openApiStartParams.getTopic().getTopic();
            kafkaSinkConfig.setTopic(topic != null ? topic : openApiStartParams.getTask());
            kafkaSinkConfig.setKafkaBootstrapServers(openApiStartParams.getTopic().getServers());
            kafkaSinkConfig.setKeySerializer("org.apache.kafka.common.serialization.StringSerializer");
            kafkaSinkConfig.setValueSerializer("org.apache.kafka.common.serialization.StringSerializer");
            // kafka 配置,默认多分区.
            kafkaSinkConfig.setOncePartitionEnabled(false);

            drcTaskVO.setKafkaSinkConfig(kafkaSinkConfig);
            sinkConfig = kafkaSinkConfig;
        }

        if ("rocketmq".equalsIgnoreCase(openApiStartParams.getTopic().getType())) {
            drcTaskVO.setSinkType(SinkConfig.Type.ROCKETMQ.getCode());

            RocketSinkConfig rocketSinkConfig = new RocketSinkConfig();
            rocketSinkConfig.setNameServer(openApiStartParams.getTopic().getServers());
            if (StringUtils.isBlank(openApiStartParams.getTopic().getTopic())) {
                throw new RuntimeException("topic 名称为空.");
            }
            rocketSinkConfig.setTopic(openApiStartParams.getTopic().getTopic());

            drcTaskVO.setRocketSinkConfig(rocketSinkConfig);

            sinkConfig = rocketSinkConfig;
        }

        if (sinkConfig == null) {
            throw new RuntimeException("topic type 输入有误.");
        }
        drcTaskVO.setSinkJson(serializer.serializer(sinkConfig));
        return drcTaskVO;
    }

}
