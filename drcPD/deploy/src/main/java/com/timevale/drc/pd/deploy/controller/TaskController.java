package com.timevale.drc.pd.deploy.controller;

import com.timevale.drc.base.dao.DrcTaskMapper;
import com.timevale.drc.base.util.CheckDataSourceValidResult;
import com.timevale.drc.base.web.BaseResult;
import com.timevale.drc.pd.service.PDTaskService;
import com.timevale.drc.pd.service.param.QpsConfigParam;
import com.timevale.drc.pd.service.vo.*;
import com.timevale.drc.pd.service.vo.universal.MyPageQueryResult;
import com.timevale.drc.pd.service.vo.universal.MyQueryResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;

/**
 * @author gwk_2
 * @date 2021/1/29 00:07
 */
@Slf4j
@RestController
@RequestMapping("/task")
@Api("DRC PD 文档")
public class TaskController {

    private TaskNameValidator taskNameValidator;
    private DbConfigValidator dbConfigValidator;

    @Autowired
    private DrcTaskMapper drcTaskMapper;
    @Autowired
    private PDTaskService taskService;

    @PostConstruct
    public void init() {
        taskNameValidator = new TaskNameValidator(drcTaskMapper);
        dbConfigValidator = new DbConfigValidator();
    }

    @ApiOperation("parent task 分页集合")
    @GetMapping("/parentList")
    public MyPageQueryResult<ParentTaskVO> parentList(@RequestParam(name = "page_num", defaultValue = "1") Integer pageNum,
                                                      @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize,
                                                      @RequestParam(name = "taskName", required = false) String taskName) {
        if (StringUtils.isNotEmpty(taskName)) {
            if (taskName.endsWith("_Incr")) {
                taskName = taskName.substring(0, taskName.length() - 5);
            }
            if (taskName.contains("_full_")) {
                int indx = taskName.indexOf("_full_");
                taskName = taskName.substring(0, indx);
            }
        }
        return taskService.parentList(pageNum, pageSize, taskName);
    }

    @ApiOperation("sub task 分页集合")
    @GetMapping("/subList")
    public MyQueryResult<List<BaseTaskVO>> subList(@RequestParam(name = "parentId") Integer parentId) {
        return new MyQueryResult<List<BaseTaskVO>>()
                .setResultObject(taskService.subList(parentId));
    }

    @ApiOperation("启动某个 task")
    @PutMapping("/start/{taskName}")
    public BaseResult start(@PathVariable("taskName") String taskName) {
        try {
            taskService.startSubTask(taskName);
        } catch (Exception e) {
            return new BaseResult(false, e.getMessage());
        }
        return new BaseResult();
    }

    @ApiOperation("关闭某个 task")
    @PutMapping("/stop/{taskName}")
    public BaseResult stop(@PathVariable("taskName") String taskName) {
        taskService.stopSubTask(taskName);
        return new BaseResult();
    }

    @ApiOperation("添加增量任务")
    @PostMapping("/addIncr")
    public BaseResult addIncrTask(@RequestBody IncrTaskInput task) {
        taskNameValidator.valid(task.getDrcTaskVO().getTaskName());
        dbConfigValidator.valid(task.getDbConfigVO().getUrl());

        taskService.addIncrTask(task);
        return new BaseResult();
    }

    @ApiOperation("添加混合库同步任务")
    @PostMapping("/addDataBaseMix")
    public BaseResult addDataBaseMix(@RequestBody MixTaskInput task) {
        taskNameValidator.valid(task.getDrcTaskVO().getTaskName());
        dbConfigValidator.valid(task.getIncrDbConfig().getUrl());
        dbConfigValidator.valid(task.getFullDbConfig().getUrl());
        taskService.addDataBaseMixTask(task);
        return new BaseResult();
    }

    @ApiOperation("添加全量任务")
    @PostMapping("/addFull")
    public BaseResult addFullTask(@RequestBody FullTaskInput input) {
        taskNameValidator.valid(input.getDrcTaskVO().getTaskName());
        dbConfigValidator.valid(input.getDbConfigVO().getUrl());
        taskService.addFullTask(input);
        return new BaseResult();
    }

    @ApiOperation("添加混合任务")
    @PostMapping("/addMix")
    public BaseResult addMixTask(@RequestBody MixTaskInput task) {
        taskNameValidator.valid(task.getDrcTaskVO().getTaskName());
        dbConfigValidator.valid(task.getFullDbConfig().getUrl());
        dbConfigValidator.valid(task.getIncrDbConfig().getUrl());
        taskService.addMixTask(task);
        return new BaseResult();
    }

    @ApiOperation("拆分全量任务并启动(会异步拆分,异步慢慢启动)")
    @PostMapping("/split/{parentTaskId}/{start}")
    public BaseResult split(@PathVariable("parentTaskId") Integer parentTaskId
            ,@ApiParam(name = "是否启动, true 表示启动") @PathVariable("start") String start) {
        taskService.split(parentTaskId, Boolean.parseBoolean(start));
        return new BaseResult();
    }

    /**
     * @param parentTaskId
     * @param opType       1 表示启动, 非1 表示停止.
     * @return
     */
    @ApiOperation("操作父任务, 1 表示启动, 2 表示停止. ")
    @GetMapping("/operateParentTask")
    public BaseResult operateParentTask(@RequestParam Integer parentTaskId,
                                        @ApiParam(value = "1表示 start, 0 表示 stop", example = "0") @RequestParam Integer opType) {
        boolean start = opType == 1;
        taskService.operateParentTask(parentTaskId, start);
        return new BaseResult();
    }

    @ApiOperation("检查 db 连接是否有效")
    @PostMapping("/checkDataSourceValid")
    public BaseResult checkDataSourceValid(@RequestBody DbConfigVO configVO) {
        CheckDataSourceValidResult result =
                taskService.checkDataSourceValid(configVO);
        return new BaseResult(result.isValid(), result.getMsg());
    }

    @ApiOperation("更新 task 的 qps 配置")
    @PostMapping("/updateQpsConfig")
    public BaseResult updateQpsConfig(@RequestBody QpsConfigParam qps) {
        taskService.updateQpsConfig(qps);
        return new BaseResult();
    }

    @ApiOperation("获取 task 的 qps 配置")
    @GetMapping("/getQpsConfig/{taskName}")
    public MyQueryResult<QPSvo> getQpsConfig(@PathVariable("taskName") String taskName) {
        QPSvo qps = taskService.getQpsConfig(taskName);
        MyQueryResult<QPSvo> q = new MyQueryResult<>();
        q.setResultObject(qps);
        return q;
    }

    @ApiOperation("删除父 task")
    @DeleteMapping("/parent/{parentId}")
    public BaseResult deleteParentTask(@PathVariable("parentId") Integer parentId) {
        log.info("删除父 task.....");
        taskService.deleteParentTask(parentId);
        return new BaseResult();
    }

    @ApiOperation("获取task 日志")
    @DeleteMapping("/getLog/{taskName}/{line}")
    public BaseResult getLog(@PathVariable("taskName") String taskName, @PathVariable("line") Integer line) throws IOException {
        String log = taskService.getLog(taskName, line);
        return new MyQueryResult<>().setResultObject(log);
    }

//    @ApiOperation("启动所有不是 running 状态的全量任务分片.")
//    @GetMapping("/startAllNotRunningFullTask/{parentTaskName}")
//    public BaseResult startAllNotRunningFullTask(@PathVariable("parentTaskName") String taskName) {
//        boolean result = taskService.startAllNotRunningFullTask(taskName);
//        return new MyQueryResult<>().setResultObject(result);
//    }

    @ApiOperation("复制任务")
    @GetMapping("/copy")
    public MyQueryResult<MixTaskInput> copy(String taskName) {
        MixTaskInput mixTaskInput = taskService.copyTask(taskName);
        return new MyQueryResult<MixTaskInput>(true, "复制成功").setResultObject(mixTaskInput);
    }

}
