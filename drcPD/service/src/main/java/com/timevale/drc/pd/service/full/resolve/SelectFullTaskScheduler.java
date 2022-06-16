package com.timevale.drc.pd.service.full.resolve;

import com.ctrip.framework.apollo.ConfigService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.timevale.drc.base.TaskStateEnum;
import com.timevale.drc.base.dao.DrcSubTaskFullSliceDetailMapper;
import com.timevale.drc.base.dao.DrcTaskMapper;
import com.timevale.drc.base.model.DrcSubTaskFullSliceDetail;
import com.timevale.drc.base.model.DrcTask;
import com.timevale.drc.base.redis.DrcLockFactory;
import com.timevale.drc.pd.service.PDServer;
import com.timevale.drc.pd.service.TaskDbOperator;
import com.timevale.drc.pd.service.worker.WorkerManager;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 全量 select 表任务调度, 防止拖垮数据库.
 * <p>
 * 如果对同一个数据库,发起过多的连接和查询,可能导致线上故障. 也可能导致 drcWorker 压力过大, CPU 告警
 * 对于同一个大的任务, 我们不能一次性启动太多小任务. 我们决定在一个 drcWorker + 一个父任务上, 默认只运行 5 个子全量任务.
 * <p>
 * 那么, 如果有 100 个父任务, 每个父任务的物理数据库连接都是同一个, 会不会造成该数据库连接爆炸呢? 答案是会的.
 * 需要解决吗? 考虑到这个场景可能不多, 先不解决.
 *
 * @author cxs
 */
@Slf4j
public class SelectFullTaskScheduler {

    private final DrcSubTaskFullSliceDetailMapper mapper;
    private final WorkerManager workerManager;
    private final PDServer pdServer;
    private final TaskDbOperator taskDbOperator;
    private final DrcLockFactory lockFactory;
    private final DrcTaskMapper drcTaskMapper;

    private ScheduledThreadPoolExecutor es =
            new ScheduledThreadPoolExecutor(1,
                    new ThreadFactoryBuilder().setNameFormat("SelectFullTaskScheduler-%d").build());

    public SelectFullTaskScheduler(DrcSubTaskFullSliceDetailMapper mapper,
                                   WorkerManager workerManager, PDServer pdServer,
                                   TaskDbOperator taskDbOperator, DrcLockFactory lockFactory,
                                   DrcTaskMapper drcTaskMapper) {
        this.mapper = mapper;
        this.workerManager = workerManager;
        this.pdServer = pdServer;
        this.taskDbOperator = taskDbOperator;
        this.lockFactory = lockFactory;
        this.drcTaskMapper = drcTaskMapper;
    }

    public void start() {
        es.scheduleAtFixedRate(() -> {
                    try {
                        lockFactory.getLock("SelectTaskScheduler").
                                lockAndProtect(1, this::scheduler);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }, 10, 10, TimeUnit.SECONDS);

    }

    public void startOne() {
        lockFactory.getLock("SelectTaskScheduler").lockAndProtect(1, this::scheduler);
    }

    public void stop() {
        es.shutdown();
    }

    public void scheduler() {
        val list = mapper.selectAllStateInit();
        int preParentId = 0;
        for (DrcSubTaskFullSliceDetail item : list) {
            if (item.getParentId().equals(preParentId)) {
                continue;
            }

            DrcTask drcTask = drcTaskMapper.selectByPrimaryKey(item.getParentId());
            if (drcTask == null) {
                continue;
            }

            final Integer parentId = item.getParentId();
            final int cunt = mapper.selectRunningCount(parentId);
            final val size = workerManager.getWorkerList().size();
            if (size == 0) {
                log.error("worker size is 0.");
                return;
            }

            // 每个 worker 负载默认 5 个正在运行的任务.
            if (cunt / size >= ConfigService.getAppConfig().getIntProperty("one.worker.load.task.num", 5)) {
                preParentId = parentId;
                continue;
            }

            try {
                log.info("准备启动全量任务 {}", item.getSubTaskName());
                pdServer.startTask(item.getSubTaskName());
                item.setState(TaskStateEnum.RUNNING.code);
                taskDbOperator.updateByPrimaryKeySelective(item);
            } catch (Exception e) {
                if (e.getCause() instanceof UndeclaredThrowableException) {
                    final UndeclaredThrowableException cause = (UndeclaredThrowableException) e.getCause();
                    final Throwable causeCause = cause.getCause();
                    log.error("启动失败 " + causeCause.getMessage());
                } else {
                    log.error("启动失败了, " + e.getCause());
                }
            }
        }
    }
}
