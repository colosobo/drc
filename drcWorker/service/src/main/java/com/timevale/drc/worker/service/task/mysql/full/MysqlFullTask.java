package com.timevale.drc.worker.service.task.mysql.full;

import com.ctrip.framework.apollo.ConfigService;
import com.timevale.drc.base.*;
import com.timevale.drc.base.binlog.Binlog2JsonModel;
import com.timevale.drc.base.dao.DrcDbConfigMapper;
import com.timevale.drc.base.dao.DrcSubTaskFullConfigMapper;
import com.timevale.drc.base.dao.DrcSubTaskFullSliceDetailMapper;
import com.timevale.drc.base.dao.DrcTaskMapper;
import com.timevale.drc.base.eventbus.DrcConsumer;
import com.timevale.drc.base.eventbus.DrcThreadPool;
import com.timevale.drc.base.eventbus.DrcThreadPoolExecutor;
import com.timevale.drc.base.eventbus.EventBus;
import com.timevale.drc.base.log.ApacheTaskLog;
import com.timevale.drc.base.log.TaskLog;
import com.timevale.drc.base.metrics.DefaultTaskMetrics;
import com.timevale.drc.base.metrics.TimeFactory;
import com.timevale.drc.base.model.DrcSubTaskFullConfig;
import com.timevale.drc.base.model.DrcSubTaskFullSliceDetail;
import com.timevale.drc.base.model.DrcTask;
import com.timevale.drc.base.util.Cost;
import com.timevale.drc.base.util.JdbcTemplateManager;
import com.timevale.drc.base.util.LogUtil;
import com.timevale.drc.worker.service.task.mysql.full.event.FullMySqlExtractOverEvent;
import com.timevale.drc.worker.service.task.mysql.full.event.UnRegisterEvent;
import com.timevale.drc.worker.service.task.mysql.incr.FailRetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author gwk_2
 * @date 2021/1/28 23:22
 */
@Slf4j
public class MysqlFullTask extends BaseTask {
    public final DrcThreadPoolExecutor doStartPool =
            DrcThreadPool.createThreadPoolWithZeroQueue(1, "full-doStartPool");

    protected final DrcSubTaskFullConfigMapper fullConfigMapper;
    protected final DrcDbConfigMapper drcDbConfigMapper;
    protected final DrcTaskMapper drcTaskMapper;
    protected final DrcSubTaskFullSliceDetailMapper drcSubTaskFullSliceDetailMapper;
    protected final JdbcTemplateManager jdbcTemplateManager;
    protected final DefaultTaskMetrics taskMetrics;

    protected Extract<List<Binlog2JsonModel>> mySqlFullExtract;
    protected final Sink<Binlog2JsonModel> sink;
    protected MySqlFullTransform mySqlFullTransform;

    protected final DrcSubTaskFullSliceDetail drcSubTaskFullSliceDetail;
    protected final int limit;
    protected int flowLimit;
    protected long updateFlowLimitTimeInMs;
    protected TaskLog taskLog;
    protected String taskName;
    protected String parentTaskName;
    protected RRateLimiter rateLimiter;
    private DrcConsumer<Task> callback;

    public MysqlFullTask(
            DrcSubTaskFullConfigMapper fullConfigMapper,
            DrcDbConfigMapper drcDbConfigMapper,
            DrcSubTaskFullSliceDetailMapper drcSubTaskFullSliceDetailMapper,
            JdbcTemplateManager jdbcTemplateManager,
            DrcSubTaskFullSliceDetail drcSubTaskFullSliceDetail,
            int limit,
            Sink<Binlog2JsonModel> sink,
            RRateLimiter rateLimiter, DrcTaskMapper drcTaskMapper) {
        this.drcTaskMapper = drcTaskMapper;
        this.fullConfigMapper = fullConfigMapper;
        this.drcDbConfigMapper = drcDbConfigMapper;
        this.drcSubTaskFullSliceDetailMapper = drcSubTaskFullSliceDetailMapper;
        this.jdbcTemplateManager = jdbcTemplateManager;
        this.drcSubTaskFullSliceDetail = drcSubTaskFullSliceDetail;
        this.limit = limit;
        this.taskMetrics = new DefaultTaskMetrics(drcSubTaskFullSliceDetail.getSubTaskName());
        this.taskName = drcSubTaskFullSliceDetail.getSubTaskName();
        this.parentTaskName = taskName.substring(0, taskName.lastIndexOf("_full_"));
        this.sink = sink;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public String getName() {
        return drcSubTaskFullSliceDetail.getSubTaskName();
    }

    @Override
    public Extract<List<Binlog2JsonModel>> getExtract() {
        if (mySqlFullExtract == null) {
            mySqlFullExtract = new MySqlFullExtract(drcSubTaskFullSliceDetail,
                    fullConfigMapper,
                    drcDbConfigMapper,
                    jdbcTemplateManager,
                    limit, taskLog);
        }
        return mySqlFullExtract;
    }

    @Override
    public Transform<Binlog2JsonModel> getTransform() {
        if (mySqlFullTransform == null) {
            mySqlFullTransform = new MySqlFullTransform();
        }
        return mySqlFullTransform;
    }

    @Override
    public Sink<Binlog2JsonModel> getSink() {
        return sink;
    }

    @Override
    public TaskLog getLog() {
        return taskLog;
    }

    @Override
    public TaskStateEnum getState() {
        return TaskStateEnum.conv(drcSubTaskFullSliceDetail.getState());
    }

    @Override
    public String getLogText(int line) {
        return LogUtil.getLogText(getName(), line);
    }

    @Override
    public void start() {
        start(null);
    }

    @Override
    public void start(DrcConsumer<Task> callback) {
        this.callback = callback;
        taskLog = new ApacheTaskLog((getName()));
        super.start();
        doStartPool.execute(() -> {
                    try {
                        taskLog = new ApacheTaskLog((getName()));
                        doStart();
                    } catch (Exception e) {
                        taskLog.error(e.getMessage(), e);
                    } finally {
                        if (this.callback != null) {
                            this.callback.accept(this);
                        }
                    }
                }
        );
    }

    @Override
    public void stop(String cause) {
        super.stop(cause);
        doStartPool.shutdown();
        if (this.callback != null) {
            callback.accept(this);
        }
    }

    @Override
    public TaskMetrics metrics() {
        return DefaultTaskMetrics.Factory.create(taskMetrics.currentQps());
    }

    protected void doStart() {
        if (drcSubTaskFullSliceDetail.getState().equals(TaskStateEnum.OVER.code)) {
            EventBus.post(new UnRegisterEvent(drcSubTaskFullSliceDetail.getSubTaskName()));
            super.stop("doStart fail");
            return;
        }
        // 运行中.
        drcSubTaskFullSliceDetail.setState(TaskStateEnum.RUNNING.code);
        drcSubTaskFullSliceDetailMapper.updateState(TaskStateEnum.RUNNING.code, drcSubTaskFullSliceDetail.getId());
        boolean broken = false;
        while (isRunning()) {
            try {
                if (fullETL()) {
                    taskLog.info("执行 etl 结束......");
                    break;
                }
            } catch (Exception e) {
                if (taskLog != null) {
                    taskLog.error(e.getMessage(), e);
                }
                log.error(e.getMessage(), e);
                broken = true;
                break;
            }
            // 更新 db cursor
            drcSubTaskFullSliceDetailMapper.updateByPrimaryKeySelective(this.drcSubTaskFullSliceDetail);
        }

        if (isRunning() && broken) {
            // 发生了异常, 什么也不做, 让"自动恢复Task"任务重启(自动恢复任务, 会发现 fail 任务状态,然后自动重启).
            if (taskLog != null) {
                taskLog.error(String.format("全量同步任务异常, %s", getName()));
            }
            log.error(String.format("全量同步任务异常, %s", getName()));
            return;
        }
        // 如果是运行时, 且没有异常, 就是自动结束
        if (isRunning() && !broken) {
            // 自动结束.
            drcSubTaskFullSliceDetail.setState(TaskStateEnum.OVER.code);
            taskLog.info(String.format("全量同步任务自动结束, %s", getName()));
            log.info(String.format("全量同步任务自动结束, %s", getName()));
            EventBus.post(new FullMySqlExtractOverEvent(drcSubTaskFullSliceDetail));
            EventBus.post(new UnRegisterEvent(drcSubTaskFullSliceDetail.getSubTaskName()));
        }
        // 如果状态被修改, 就是手动停止.
        if (!isRunning()) {
            // 如果状态被修改了, 就是手动停止.
            drcSubTaskFullSliceDetail.setState(TaskStateEnum.HAND_STOP.code);
            taskLog.info(String.format("全量同步任务手动结束, %s", getName()));
            log.info(String.format("全量同步任务手动结束, %s", getName()));
        }
        drcSubTaskFullSliceDetailMapper.updateByPrimaryKeySelective(drcSubTaskFullSliceDetail);
        sink.stop();
    }

    private boolean fullETL() {
        Cost cost = Cost.start();
        List<Binlog2JsonModel> list = getExtract().extract();
        long extractCost = cost.end();

        // 记录提取耗时
        String format = String.format("select list size = %d ,select cost : %d", list.size(), extractCost);
        taskLog.info(format);

        if (list.size() <= 0) {
            taskLog.info("MysqlFullTask 结束.");
            return true;
        }
        // 发送.
        FailRetryUtil.failRetry(this, () -> sink(list), e -> {
            log.error("sink 失败, 准备重试", e);
            getLog().info("sink 失败, 准备重试" + e.getMessage(), e);
        });

        updateCursor(list.get(list.size() - 1));

        return false;
    }

    protected void sink(List<Binlog2JsonModel> list) {

        // limit 配置. 2 层配置.
        Integer limit = ConfigService.getAppConfig().getIntProperty("batch.insert.limit." + this.parentTaskName, null);
        if (limit == null) {
            limit = ConfigService.getAppConfig().getIntProperty("batch.insert.limit", 50);
        }
        // sink
        if (list.size() <= limit) {
            sinkAndLimitAndStat(list);
        } else {
            for (int i = 0; i < list.size(); i += limit) {
                List<Binlog2JsonModel> ll = new ArrayList<>();
                if (list.size() - i > limit) {
                    ll.addAll(list.subList(i, limit + i));
                } else {
                    ll.addAll(list.subList(i, list.size()));
                }
                sinkAndLimitAndStat(ll);
            }
        }
    }

    protected void redissonLimit(int ac) {
        if (flowLimit == 0 || updateFlowLimitTimeInMs == 0 ||
                TimeFactory.currentTimeMillis() - updateFlowLimitTimeInMs > TimeUnit.SECONDS.toMillis(5)) {
            DrcSubTaskFullSliceDetail dbModel = drcSubTaskFullSliceDetailMapper.selectByPrimaryKey(this.drcSubTaskFullSliceDetail.getId());
            DrcSubTaskFullConfig drcSubTaskFullConfig = fullConfigMapper.selectByPrimaryKey(dbModel.getDrcSubTaskFullConfigId());
            Integer drcTaskId = drcSubTaskFullConfig.getDrcTaskId();
            DrcTask drcTask = drcTaskMapper.selectByPrimaryKey(drcTaskId);
            Integer qpsLimitConfig = drcTask.getQpsLimitConfig();
            if (!qpsLimitConfig.equals(flowLimit)) {
                flowLimit = qpsLimitConfig;
                // update
                rateLimiter.setRate(RateType.OVERALL, flowLimit, 1, RateIntervalUnit.SECONDS);
            }
            updateFlowLimitTimeInMs = TimeFactory.currentTimeMillis();
        }
        if (ac > flowLimit) {
            ac = flowLimit;
        }

        rateLimiter.acquire(ac);
    }

    protected void updateCursor(Binlog2JsonModel model) {
        String rowKey = model.getRowKey();
        Object value = model.getAfter().get(rowKey);
        drcSubTaskFullSliceDetail.setSliceCursor(value.toString());
    }

    public void setRunning() {
        super.running = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MysqlFullTask that = (MysqlFullTask) o;
        return taskName.equalsIgnoreCase(that.taskName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskName);
    }

    private void sinkAndLimitAndStat(List<Binlog2JsonModel> list) {

        redissonLimit(list.size());

        Cost start = Cost.start();

        FailRetryUtil.failRetry(this, () -> sink.sink(list), e -> {
            log.error("sinkAndLimitAndStat fail ", e);
            getLog().info("sinkAndLimitAndStat fail " + e.getMessage(), e);
        });

        taskLog.info("批量插入耗时 = " + start.end() + "ms, size = " + list.size());

        taskMetrics.stat(list.size());
    }

}
