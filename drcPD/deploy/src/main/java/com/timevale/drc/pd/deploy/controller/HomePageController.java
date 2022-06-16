package com.timevale.drc.pd.deploy.controller;

import com.timevale.drc.base.model.QpsLog;
import com.timevale.drc.base.web.BaseResult;
import com.timevale.drc.pd.service.PDTaskService;
import com.timevale.drc.pd.service.stat.ClusterSummary;
import com.timevale.drc.pd.service.vo.SimpleWorkerVO;
import com.timevale.drc.pd.service.vo.TaskSummaryVO;
import com.timevale.drc.pd.service.vo.universal.MyQueryResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author gwk_2
 * @date 2021/3/17 15:49
 */
@Slf4j
@RestController
@RequestMapping("/home")
@Api("DRC PD 首页文档")
public class HomePageController {
    @Autowired
    private PDTaskService taskService;

    @ApiOperation("worker 集合")
    @GetMapping("/list")
    public MyQueryResult<List<SimpleWorkerVO>> list() {
        MyQueryResult<List<SimpleWorkerVO>> objectListResult = new MyQueryResult<>();
        try {
            objectListResult.setResultObject(taskService.workerList());
        } catch (Exception e) {
            return new MyQueryResult<>(false, e.getMessage());
        }
        return objectListResult;
    }

    @ApiOperation("将某个 task 转移到指定 机器")
    @GetMapping("/failover")
    public BaseResult failover(@RequestParam("taskName") String taskName,
                               @RequestParam("workerTcpUrl") String workerTcpUrl) {
        log.info("手动触发故障转移, 将 {} 转移到 {}", taskName, workerTcpUrl);
        taskService.failover(taskName, workerTcpUrl);
        return new BaseResult();
    }


    @ApiOperation("转移目标 pod 的任务到其他的 pod 上.")
    @GetMapping("/transferTaskList")
    public MyQueryResult<Boolean> transferTaskList(@RequestParam("podIp") String podIp){
        log.info("转移目标 pod 的任务到其他的 pod 上.");
        taskService.failover(podIp);
        return new MyQueryResult<Boolean>(true, "failover 成功").setResultObject(true);
    }


    @ApiOperation("执行自动负载均衡")
    @GetMapping("/autoReBalance")
    public BaseResult autoReBalance() {
        taskService.autoReBalance();
        return new BaseResult();
    }

    @ApiOperation("正在运行的 task 数量")
    @GetMapping("/runningTaskCount")
    public MyQueryResult<TaskSummaryVO> runningTaskCount() {
        MyQueryResult<TaskSummaryVO> r = new MyQueryResult<>();
        r.setResultObject(taskService.runningTaskCount());
        return r;
    }

    @ApiOperation("获取集群总状态")
    @GetMapping("/clusterSummary")
    public MyQueryResult<ClusterSummary> clusterState() {
        MyQueryResult<ClusterSummary> r = new MyQueryResult<>();
        r.setResultObject(taskService.clusterSummary());
        return r;
    }

    @ApiOperation("获取QPS 集合, start end ,单位秒")
    @GetMapping("/qpsChart/{start}/{end}")
    public MyQueryResult<List<QpsLog>> qpsChart(@PathVariable("start") String s, @PathVariable("end") String e) {
        Long start = Long.parseLong(s);
        Long end = Long.parseLong(e);
        MyQueryResult<List<QpsLog>> r = new MyQueryResult<>();
        r.setResultObject(taskService.getQpsList(start, end));
        return r;
    }

}
