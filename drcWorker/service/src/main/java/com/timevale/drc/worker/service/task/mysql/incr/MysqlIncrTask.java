package com.timevale.drc.worker.service.task.mysql.incr;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.timevale.drc.base.*;
import com.timevale.drc.base.alarm.AlarmUtil;
import com.timevale.drc.base.dao.DrcDbConfigMapper;
import com.timevale.drc.base.dao.DrcSubTaskIncrMapper;
import com.timevale.drc.base.dao.DrcTaskMapper;
import com.timevale.drc.base.eventbus.DrcConsumer;
import com.timevale.drc.base.eventbus.DrcThreadPool;
import com.timevale.drc.base.eventbus.DrcThreadPoolExecutor;
import com.timevale.drc.base.eventbus.EventBus;
import com.timevale.drc.base.log.ApacheTaskLog;
import com.timevale.drc.base.log.TaskLog;
import com.timevale.drc.base.metrics.DefaultTaskMetrics;
import com.timevale.drc.base.metrics.TimeFactory;
import com.timevale.drc.base.model.DrcSubTaskIncr;
import com.timevale.drc.base.model.DrcTask;
import com.timevale.drc.base.redis.DrcRedisson;
import com.timevale.drc.base.rocketmq.admin.MQAdminService;
import com.timevale.drc.base.util.DrcZkClient;
import com.timevale.drc.base.util.LogUtil;
import com.timevale.drc.base.util.MysqlIncrTaskConstants;
import com.timevale.drc.worker.service.RestartController;
import com.timevale.drc.worker.service.canal.DrcCanalServer;
import com.timevale.drc.worker.service.canal.StagingSink;
import com.timevale.drc.worker.service.canal.support.DrcLogAlarmHandler;
import com.timevale.drc.worker.service.task.Coordinator;
import com.timevale.drc.worker.service.task.mysql.full.event.UnRegisterEvent;
import com.timevale.drc.worker.service.task.mysql.incr.support.MessageHandler;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.timevale.drc.base.rocketmq.admin.MQConstants.DRC_MQ_REPLAY_CONSUMER_GROUP_PREFIX;
import static com.timevale.drc.base.util.MysqlIncrTaskConstants.*;

/**
 * ??????????????????, ?????????, runningSink ????????? sink.
 * ?????????????????????, ?????????????????????. MQ ????????? realSink(?????????Sink) ??????.
 * ???????????????, checking ?????? realSink ????????? runningSink, ??????????????? Sink ??????.
 *
 * @author gwk_2
 * @date 2021/1/28 23:22
 */
@Getter
public class MysqlIncrTask<M> extends BaseTask {

    private static final Logger logger = LoggerFactory.getLogger(MysqlIncrTask.class);

    private static final Set<Integer> MIX_TYPE_SET = Sets.newHashSet(TaskTypeEnum.MYSQL_MIX_TASK.code, TaskTypeEnum.MYSQL_DATABASE_MIX_TASK.code);
    private final DrcThreadPoolExecutor asyncStartPool;
    protected final DrcThreadPoolExecutor workPool;
    protected final DrcThreadPoolExecutor replayPool;

    private final DrcSubTaskIncr dbTaskDO;
    private final DrcTask parentTask;
    private final DrcSubTaskIncrMapper incrTaskMapper;
    private final DrcTaskMapper drcTaskMapper;
    private final DrcDbConfigMapper dbConfigMapper;
    private final MQAdminService mqAdminService;
    private final DrcZkClient drcZkClient;
    private final Coordinator coordinator;

    Sink<M> runningSink;
    Sink<M> stagingSink;
    Sink<M> directSink;
    protected MysqlIncrTaskReplay<M> replay;
    protected Transform<M> transform;

    protected final String runningStatusKey;

    private final String drcZkAddr;
    private final DefaultTaskMetrics taskMetrics;
    private final TaskLog taskLog;
    private final MysqlIncrTaskChecking mysqlIncrTaskChecking = MysqlIncrTaskChecking.getInstance();
    private final String taskName;

    protected Extract<List<M>> extract;
    private final DrcCanalServer drcCanalServer;
    private final Properties properties;

    private final MessageHandler<M> messageHandler;
    private final String backUpConsumerGroup;

    private DrcConsumer<Task> callback;

    private int flowLimit;
    private long updateFlowLimitTimeInMs;
    final RRateLimiter rRateLimiter;
    private boolean watched;

    /**
     * ?????????????????????????????????
     */
    protected volatile AtomicBoolean paused = new AtomicBoolean(true);

    public MysqlIncrTask(MessageHandler<M> messageHandler,
                         String drcZkAddr,
                         DrcSubTaskIncr dbTaskDO,
                         DrcTask parentTask,
                         DrcSubTaskIncrMapper taskMapper,
                         DrcDbConfigMapper dbConfigMapper,
                         Sink<M> realSink,
                         MQAdminService mqAdminService,
                         DrcZkClient drcZkClient,
                         Coordinator coordinator,
                         Properties properties,
                         RRateLimiter rRateLimiter, DrcTaskMapper drcTaskMapper,
                         Transform<M> transform,
                         DrcLogAlarmHandler drcLogAlarmHandler) {
        this.messageHandler = messageHandler;
        this.dbTaskDO = dbTaskDO;
        this.taskName = dbTaskDO.getSubTaskName();
        this.taskLog = new ApacheTaskLog(getName());
        this.drcZkAddr = drcZkAddr;
        this.properties = properties;
        this.parentTask = parentTask;
        this.incrTaskMapper = taskMapper;
        this.dbConfigMapper = dbConfigMapper;
        this.mqAdminService = mqAdminService;
        this.drcZkClient = drcZkClient;
        this.coordinator = coordinator;
        this.runningSink = realSink;
        this.runningStatusKey = MysqlIncrTaskConstants.getRunningStatus(taskName);
        this.workPool = DrcThreadPool.createThreadPoolWithZeroQueue(1, "incr-task-work");
        this.asyncStartPool = DrcThreadPool.createThreadPoolWithZeroQueue(1, "incr-task-doStart-" + taskName);
        this.replayPool = DrcThreadPool.createThreadPoolWithZeroQueue(1, "incr-task-replay-" + taskName);
        this.taskMetrics = new DefaultTaskMetrics(taskName);
        this.backUpConsumerGroup = DRC_MQ_REPLAY_CONSUMER_GROUP_PREFIX + taskName;
        this.rRateLimiter = rRateLimiter;
        this.drcTaskMapper = drcTaskMapper;
        this.drcCanalServer = DrcCanalServer.getInstanceWithStart(drcZkAddr, drcZkClient, drcLogAlarmHandler);
        this.transform = transform;
        this.extract = new MySqlIncrExtract<>(this, drcCanalServer, messageHandler, taskMetrics);
    }

    @Override
    public synchronized void stop(String cause) {

        if (dbTaskDO.getState() == TaskStateEnum.HAND_STOP.code) {
            taskLog.info("stop ??????, ???????????????????????????......");
            return;
        }

        super.stop(cause);

        asyncStartPool.shutdown();
        replayPool.shutdown();
        workPool.shutdownNow();
//        if (watched) {
//            coordinator.unWatch(parentTask.getTaskName());
//        }
    }

    @Override
    public TaskMetrics metrics() {
        return TaskMetrics.Factory.create(taskMetrics.currentQps());
    }

    private void doStop() {
        taskLog.info(taskName + " doStop");
        // ??????canalConnector
        if (drcCanalServer != null) {
            taskLog.info("?????? canal Instance, TaskName = " + getName());
            drcCanalServer.stopInstance(getName());
        }

        // ????????????
        if (parentTask.getTaskType() == TaskTypeEnum.MYSQL_MIX_TASK.code && replay != null) {
            taskLog.info("??????????????????????????? consumer,  TaskName = " + getName());
            replay.stop();
        }

        // ??????check??????
        if (parentTask.getTaskType() == TaskTypeEnum.MYSQL_MIX_TASK.code) {
            if (mysqlIncrTaskChecking.removeTask(this)) {
                taskLog.info("stop, ????????????????????????????????????check????????????,  TaskName = " + getName());
            }
        }

        if (callback != null) {
            callback.accept(this);
        }

        taskLog.info(taskName + " doStop success.");
    }

    @Override
    public void start() {
        start(null);
    }

    @Override
    public synchronized void start(DrcConsumer<Task> callback) {

        if (isRunning()) {
            taskLog.info("Task is already running, Don't start again,  TaskName = " + getName());
            return;
        }

        if (drcCanalServer != null && drcCanalServer.isStart(getTaskName())) {
            taskLog.info("Task DrcCanalServer is already running, Don't start again,  TaskName = " + getName());
            return;
        }

        this.callback = callback;
        super.start();

        doStart();
    }

    public void doStart() {
        try {
            // 1 ??????????????????????????????????????????
            if (dbTaskDO.getState() == TaskStateEnum.INIT.code) {
                // ??????????????????mq topic???canal server instance
                try {
                    initResourcesByFirstStartTask();
                } catch (Exception e) {
                    AlarmUtil.pushAlarm2Admin("??????????????????mq topic???canal server instance,  TaskName = " + getName());
                    taskLog.error("??????????????????mq topic???canal server instance, ??????:", e);
                    stop("initResourcesByFirstStartTask fail");
                    return;
                }
            }
            // 2 ?????????????????????????????????
            dbTaskDO.setState(TaskStateEnum.RUNNING.code);
            incrTaskMapper.updateByPrimaryKeySelective(dbTaskDO);

            // 3 ????????????????????????
            try {
                initRuntimeResources();
            } catch (Exception e) {
                taskLog.error("??????????????????????????????, ??????:", e);
                logger.error("??????????????????????????????, ??????:", e);
                dbTaskDO.setState(TaskStateEnum.EXCEPTION.code);
                incrTaskMapper.updateByPrimaryKeySelective(dbTaskDO);
                stop("initRuntimeResources fail, " + e.getMessage());
                return;
            }

            // 4 ??????check??????
            String status = getRunningStatus();
            if (MIX_TYPE_SET.contains(parentTask.getTaskType()) && !RUNNING_STATUS_DIRECT.equals(status)) {
                // ???????????????????????????, ????????????????????????. ??????????????????, ???????????????????????????.
                paused.set(true);
                mysqlIncrTaskChecking.addTask(this);
            } else {
                // ????????????????????????, ???????????????..
                paused.set(false);
            }

            // 5 ??????????????????????????????
            if (RUNNING_STATUS_STASHING.equals(status)) {
                taskLog.info("????????????????????????.");
                coordinator.watch(parentTask.getTaskName(), () -> {
                    // ??????????????????
                    taskLog.info("????????????????????????, ?????? rocketmq consumer,  TaskName = " + getName());
                    setRunningStatus(RUNNING_STATUS_REPLAYING);
                    replay.replay();
                });
            }

            // 6 ????????????.
            workPool.execute(MysqlIncrTask.this::binlogETL);
        } catch (Exception e) {
            taskLog.error("???????????? Binlog Task ??????, ?????????.", e);
            AlarmUtil.pushAlarm2Admin("???????????? Binlog Task ??????, ?????????, Task = " + getName());
            // ??????????????????. ?????? UnRegisterEvent ??????.
            EventBus.post(new UnRegisterEvent(getName()));
        }
    }

    private void binlogETL() {
        while (isRunning()) {
            try {
                if (paused.get()) {
                    taskLog.info(String.format("???????????? = %s, paused = %s", getRunningStatus(), paused.get()));
                    TimeUnit.SECONDS.sleep(1);
                    continue;
                }

                // etl??????
                List<M> msgList = extract.extract();

                if (CollectionUtils.isEmpty(msgList)) {
                    extract.ack();
                    continue;
                }

                for (M msg : msgList) {
                    limit();
                    taskMetrics.stat();
                    M m = transform.transform(msg);
                    FailRetryUtil.failRetry(this, () -> runningSink.sink(Lists.newArrayList(m)), e -> getLog().info(e.getMessage()));
                }

                taskLog.info("???????????? binlog etl ..... ?????????=" + msgList.size() + "," + getTaskName() + " runningSink = " + runningSink.getName());
                extract.ack();
            } catch (InterruptedException e1) {
                // ignored
            } catch (Exception e) {
                taskLog.error("doingEtl ??????, e:", e);
                RestartController.putRestartTask(this.taskName);
            }
        }
        doStop();
    }

    private void limit() {

        int qps = taskMetrics.currentQps();
        // ????????????????????????????????? QPS,?????????????????????, ???????????????.???????????? Redis ?????????????????????.
        if (flowLimit > qps) {
            return;
        }

        // 5s ???????????????????????????????????????.
        if (flowLimit == 0 || updateFlowLimitTimeInMs == 0 || TimeFactory.currentTimeMillis() - updateFlowLimitTimeInMs > TimeUnit.SECONDS.toMillis(5)) {
            DrcSubTaskIncr dbModel = incrTaskMapper.selectByPrimaryKey(this.dbTaskDO.getId());
            DrcTask drcTask = drcTaskMapper.selectByPrimaryKey(dbModel.getParentId());
            updateFlowLimitTimeInMs = TimeFactory.currentTimeMillis();
            flowLimit = drcTask.getQpsLimitConfig();
            rRateLimiter.setRate(RateType.OVERALL, flowLimit, 1, RateIntervalUnit.SECONDS);
        }
        rRateLimiter.acquire();
    }

    private void initRuntimeResources() {

        // ???????????????
        String runningStatus = getRunningStatus();

        // ???????????????, ??????, ????????????,
        if (!RUNNING_STATUS_DIRECT.equals(runningStatus)) {
            // ?????????.
            directSink = runningSink;
            // ?????????sink
            stagingSink = new StagingSink<>(getName());
            stagingSink.start();
            // ??????
            runningSink = stagingSink;
            replay = new MysqlIncrTaskReplay<>(mqAdminService, this);
        }

        // ????????????????????????
        if (runningStatus.equals(RUNNING_STATUS_REPLAYING) || runningStatus.equals(RUNNING_STATUS_STASH_PAUSED)) {
            new Thread(() -> replay.replay()).start();
        }
    }

    String getRunningStatus() {
        String status = DrcRedisson.get(runningStatusKey);
        if (StringUtils.isBlank(status)) {
            DrcRedisson.set(runningStatusKey, RUNNING_STATUS_DIRECT);
            status = RUNNING_STATUS_DIRECT;
        }
        return status;
    }

    synchronized void setRunningStatus(String runningStatus) {
        if (StringUtils.isBlank(runningStatus)) {
            throw new RuntimeException("???????????????????????? runningStatus, ?????????.");
        }
        DrcRedisson.set(runningStatusKey, runningStatus);
    }

    private void initResourcesByFirstStartTask() {
        // ?????????????????????;
        String taskName = getName();
        if (MIX_TYPE_SET.contains(parentTask.getTaskType())) {
            // ??????+????????????????????????STASHING
            taskLog.info("???????????????, ??????????????????????????????????????????,  TaskName = " + getName());
            setRunningStatus(RUNNING_STATUS_STASHING);
            // ??????mq topic/consumer group
            mqAdminService.createMQTopicAndConsumerGroup(taskName);
        } else {
            // ???????????????????????????DIRECT
            taskLog.info("?????????????????????????????????????????????,  TaskName = " + getName());
            setRunningStatus(RUNNING_STATUS_DIRECT);
        }

    }

    @Override
    public String getName() {
        return dbTaskDO.getSubTaskName();
    }

    @Override
    public TaskStateEnum getState() {
        Integer state = dbTaskDO.getState();
        if (state != TaskStateEnum.RUNNING.code) {
            return TaskStateEnum.conv(state);
        }

        boolean start = drcCanalServer.isStart(getName());
        // ?????????????????????, ????????????.
        if (!start) {
            return TaskStateEnum.EXCEPTION;
        }

        String runningStatus = getRunningStatus();
        if (runningStatus == null || StringUtils.equals(runningStatus, RUNNING_STATUS_DIRECT)) {
            return TaskStateEnum.RUNNING;
        }
        if (StringUtils.equals(runningStatus, RUNNING_STATUS_STASHING)) {
            return TaskStateEnum.STAGING;
        }
        return TaskStateEnum.PLAYBACK_ING;
    }

    @Override
    public String getLogText(int line) {
        return LogUtil.getLogText(getName(), line);
    }

    @Override
    public Extract<List<M>> getExtract() {
        return extract;
    }

    @Override
    public Transform<M> getTransform() {
        return transform;
    }

    @Override
    public Sink<M> getSink() {
        return runningSink;
    }

    @Override
    public TaskLog getLog() {
        return taskLog;
    }

    public void stopReplay() {
        if (replay == null) {
            return;
        }
        replay.stop();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MysqlIncrTask<?> that = (MysqlIncrTask<?>) o;
        return taskName.equals(that.taskName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskName);
    }
}
