package com.timevale.drc.pd.service.worker;

import com.ctrip.framework.apollo.ConfigService;
import com.timevale.drc.base.Task;
import com.timevale.drc.base.TaskStateEnum;
import com.timevale.drc.base.TaskTypeEnum;
import com.timevale.drc.base.alarm.AlarmUtil;
import com.timevale.drc.base.dao.DrcSubTaskFullSliceDetailMapper;
import com.timevale.drc.base.dao.DrcSubTaskIncrMapper;
import com.timevale.drc.base.dao.DrcTaskMapper;
import com.timevale.drc.base.eventbus.DrcThreadPool;
import com.timevale.drc.base.model.DrcSubTaskFullSliceDetail;
import com.timevale.drc.base.model.DrcSubTaskIncr;
import com.timevale.drc.base.model.DrcTask;
import com.timevale.drc.base.redis.DrcLockFactory;
import com.timevale.drc.pd.service.PDServer;
import com.timevale.drc.pd.service.PDTaskService;
import com.timevale.drc.pd.service.exp.NotFoundWorkerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * 自动恢复 运行时异常的任务.
 * <p>
 * 1 分钟调度一次, 比较合理.
 *
 * @author gwk_2
 * @date 2021/4/26 21:12
 */
@Component
@Slf4j
public class AutoRecoverTask {

    @Value("${env:test}")
    private String env;

    @Autowired
    private DrcTaskMapper drcTaskMapper;
    @Autowired
    private DrcSubTaskIncrMapper incrMapper;
    @Autowired
    private DrcSubTaskFullSliceDetailMapper fullSliceDetailMapper;
    @Autowired
    private PDServer pdServer;
    @Autowired
    private PDTaskService pdTaskService;
    @Autowired
    private DrcLockFactory lockFactory;

    ScheduledExecutorService scheduledExecutorService = DrcThreadPool.newScheduledThreadPool(1, AutoRecoverTask.class.getCanonicalName());

    @PreDestroy
    public void shutDown() {
        scheduledExecutorService.shutdown();
    }

    @PostConstruct
    public void init() {
        scheduledExecutorService.scheduleAtFixedRate(() -> {
                    try {
                        // 抢到锁就执行, 不做任何等待.
                        lockFactory.getLock("AutoRecoverTask").lockAndProtect(0, this::execute);
                        // 一分钟一次.
                    } catch (Exception e) {
                        // ignore
                        log.info("AutoRecoverTask, msg=" + e.getMessage());
                    }
                }, 1, 5, TimeUnit.SECONDS);
    }

    public void execute() {

        if (ConfigService.getAppConfig().getBooleanProperty("AutoRecoverTask.enabled", false)) {
            return;
        }

        List<DrcTask> allTask = drcTaskMapper.list();

        for (DrcTask drcTask : allTask) {
            Integer id = drcTask.getId();
            Integer taskType = drcTask.getTaskType();

            if (taskType == TaskTypeEnum.MYSQL_FULL_TASK.code) {
                recoverFullTask(id);
            }

            if (taskType == TaskTypeEnum.MYSQL_INCR_TASK.code) {
                recoverIncrTask(id);
            }

            if (taskType == TaskTypeEnum.MYSQL_MIX_TASK.code) {
                recoverFullTask(id);
                recoverIncrTask(id);
            }

            if (taskType == TaskTypeEnum.MYSQL_DATABASE_MIX_TASK.code) {
                recoverFullTask(id);
                recoverIncrTask(id);
            }
        }
    }

    private void recoverIncrTask(Integer id) {
        DrcSubTaskIncr drcSubTaskIncr = incrMapper.selectByParentId(id);

        Integer dbState = drcSubTaskIncr.getState();
        if (dbState == TaskStateEnum.RUNNING.code || dbState == TaskStateEnum.EXCEPTION.code) {
            tryRecoverTask(drcSubTaskIncr.getSubTaskName());
        }
    }

    private void recoverFullTask(Integer id) {
        List<DrcSubTaskFullSliceDetail> detailList = fullSliceDetailMapper.selectByParentId(id);
        for (DrcSubTaskFullSliceDetail item : detailList) {
            Integer dbState = item.getState();
            if (dbState == TaskStateEnum.RUNNING.code || dbState == TaskStateEnum.EXCEPTION.code) {
                tryRecoverTask(item.getSubTaskName());
            }
        }
    }

    private void tryRecoverTask(String taskName) {
        try {

            boolean broken = true;
            TaskStateEnum state = null;
            Task task = pdServer.getTaskManager().getTaskWithOutCreate(taskName);
            if (task != null) {
                try {
                    // 可能 rpc 异常.
                    state = task.getState();
                    broken = false;
                } catch (Exception e) {
                    log.error("状态异常, taskName{}, msg={}", taskName, e.getMessage());
                }
            }
            if (broken || state == TaskStateEnum.EXCEPTION || state == TaskStateEnum.DB_RUNNING_RPC_EXCEPTION) {
                pdTaskService.startSubTask(taskName);
                log.info(env + " 环境, 任务异常, 故障自愈成功, 任务 = {}", taskName);
            }
        } catch (NotFoundWorkerException notFoundWorkerException) {
            // 可能是网络问题. 先忽略.
            throw notFoundWorkerException;
        } catch (Exception e) {
            log.error(env + " 环境, task 异常自愈失败, 请及时查看, task = " + taskName + ", msg=" + e.getMessage());
            if (env.equalsIgnoreCase("prod")) {
                AlarmUtil.pushAlarm2Admin(env + " 环境, task 异常自愈失败, 请及时查看, task = " + taskName);
            }
        }
    }
}
