package com.timevale.drc.worker.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.timevale.drc.base.Task;
import com.timevale.drc.base.metrics.TaskMetricsModel;
import com.timevale.drc.pd.facade.api.PdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author gwk_2
 * @date 2022/3/8 10:57
 */
@Component
@Slf4j
public class MetricsServiceScheduler {

    @Autowired
    private PdService pdService;
    @Autowired
    private WorkerServer workerServer;

    private static final int CORE_SIZE = 1;

    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =
            new ScheduledThreadPoolExecutor(CORE_SIZE, new ThreadFactoryBuilder().setNameFormat("worker-metricsService").build());

    @PostConstruct
    public void init() {
        scheduledThreadPoolExecutor.scheduleWithFixedDelay(
                this::uploadTaskMetrics, 6, 995, TimeUnit.MILLISECONDS);
    }

    public void uploadTaskMetrics() {
        try {

            Map<String, TaskMetricsModel> data = new HashMap<>(16);

            Collection<Task> taskList = workerServer.getTaskService().getTaskList();

            for (Task task : taskList) {
                task.metrics().currentQps();
                TaskMetricsModel taskMetricsModel = new TaskMetricsModel();
                taskMetricsModel.setName(task.getName());
                taskMetricsModel.setQps(task.metrics().currentQps());
                data.put(task.getName(), taskMetricsModel);
            }

            if (data.size() > 0) {
                pdService.uploadTaskMetrics(data);
            }
        } catch (Exception e) {
            log.warn("上传指标资源失败 {}", e.getMessage());
        }
    }
}
