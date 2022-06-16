package com.timevale.drc.pd.service.full.resolve;

import com.timevale.drc.base.dao.DrcSubTaskFullConfigMapper;
import com.timevale.drc.base.eventbus.DrcThreadPool;
import com.timevale.drc.base.eventbus.DrcThreadPoolExecutor;
import com.timevale.drc.base.model.DrcSubTaskFullConfig;
import com.timevale.drc.base.redis.DrcLockFactory;
import com.timevale.drc.base.redis.LockNotAcquiredException;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.timevale.drc.base.model.DrcSubTaskFullConfig.SET_SPLIT_STATE_ING;
import static com.timevale.drc.base.model.DrcSubTaskFullConfig.SPLIT_STATE_NO;

/**
 * @author gwk_2
 * @date 2022/4/14 22:02
 */
@Slf4j
public class SplitDispatch {

    public LinkedBlockingQueue<DrcSubTaskFullConfig> channel = new LinkedBlockingQueue<>();

    public volatile int state = 0;

    private final DrcSubTaskFullConfigMapper fullConfigMapper;
    private final SplitSliceTaskFactory splitSliceTaskFactory;
    private final DrcLockFactory lockFactory;
    private SelectFullTaskScheduler selectFullTaskScheduler;

    private final DrcThreadPoolExecutor drcThreadPool =
            DrcThreadPool.createThreadPoolWithZeroQueue(1, "SplitDispatch");


    public SplitDispatch(DrcSubTaskFullConfigMapper fullConfigMapper,
                         SplitSliceTaskFactory splitSliceTaskFactory,
                         DrcLockFactory lockFactory,
                         SelectFullTaskScheduler selectFullTaskScheduler) {

        this.fullConfigMapper = fullConfigMapper;
        this.splitSliceTaskFactory = splitSliceTaskFactory;
        this.lockFactory = lockFactory;
        this.selectFullTaskScheduler = selectFullTaskScheduler;
    }

    public void start() {
        drcThreadPool.execute(this::dispatch);
    }

    public void stop() {
        state = 1;
        drcThreadPool.shutdown();
    }

    public void putTask(DrcSubTaskFullConfig config) {
        channel.offer(config);
    }

    public void dispatch() {
        while (state == 0) {
            run0();
        }
    }

    private void run0() {
        DrcSubTaskFullConfig item = new DrcSubTaskFullConfig();
        try {
            // 注意, 拆分如果超过 5 分钟, 可能会触发 ReloadFailSplitTask 任务.
            item = channel.poll(3, TimeUnit.SECONDS);
            if (item == null) {
                return;
            }
            DrcSubTaskFullConfig finalItem = item;

            // 防止 ReloadFailSplitTask 并发操作.
            lockFactory.getLock("SplitDispatch_" + item.getId()).
                    lockAndProtect(1, () -> {
                        final DrcSubTaskFullConfig db = fullConfigMapper.selectByPrimaryKey(finalItem.getId());
                        // 防止并发操作.
                        if (db.getSplitState() != SPLIT_STATE_NO) {
                            return;
                        }
                        finalItem.setSplitState(SET_SPLIT_STATE_ING);
                        fullConfigMapper.updateStateRunningById(finalItem.getId());
                        // 如果这里宕机, 那么, 拆分状态为0 或 1,就需要定时任务捞起重来.
                        // @see ReloadFailSplitTask
                        // 串行拆分, 减轻数据库连接压力. 拆分后, SelectTaskScheduler 会进行调度执行.
                        SplitSliceTask splitSliceTask = splitSliceTaskFactory.create(finalItem, selectFullTaskScheduler);
                        //splitSliceTask.setCallback(new StartTaskCallback(fullSliceDetailMapper, pdServer, taskDbOperator));
                        splitSliceTask.run();
                    });

        } catch (LockNotAcquiredException e1) {
            // ignore
        } catch (Exception e) {
            log.error("拆分出现异常, {} ", item.getTableName(), e);
        }
    }
}
