package com.timevale.drc.pd.service.full.resolve;

import com.beust.jcommander.internal.Lists;
import com.timevale.drc.base.TaskStateEnum;
import com.timevale.drc.base.alarm.AlarmUtil;
import com.timevale.drc.base.dao.*;
import com.timevale.drc.base.eventbus.DrcConsumer;
import com.timevale.drc.base.eventbus.DrcThreadPool;
import com.timevale.drc.base.eventbus.DrcThreadPoolExecutor;
import com.timevale.drc.base.model.*;
import com.timevale.drc.base.util.Cost;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.skywalking.apm.toolkit.trace.TraceCrossThread;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.timevale.drc.base.model.DrcSubTaskFullSliceDetail.IS_LAST_SLICE_FALSE;
import static com.timevale.drc.base.model.DrcSubTaskFullSliceDetail.IS_LAST_SLICE_TRUE;

/**
 * @author gwk_2
 */
@Slf4j
@Setter
@TraceCrossThread
public class SplitSliceTask {

    private final DrcTaskMapper drcTaskMapper;

    private DrcSubTaskFullConfigMapper drcSubTaskFullConfigMapper;
    private final DrcSubTaskFullConfig fullConfig;
    private final DrcDbConfigMapper drcDbConfigMapper;
    private final Integer limit;
    private DrcSubTaskFullSliceDetailMapper drcSubTaskFullSliceDetailMapper;
    private DrcSubTaskSchemaLogMapper drcSubTaskSchemaLogMapper;
    private LinkedBlockingQueue<Slice> channel = new LinkedBlockingQueue<>();
    private DrcThreadPoolExecutor startPool = DrcThreadPool.createThreadPoolWithZeroQueue(1, "startPool");
    private DrcThreadPoolExecutor splitPool = DrcThreadPool.createThreadPoolWithZeroQueue(1, "splitPool");
    private DrcConsumer<Void> consumer;

    public SplitSliceTask(DrcConsumer<Void> consumer,
            DrcSubTaskFullConfigMapper drcSubTaskFullConfigMapper,
                          DrcTaskMapper drcTaskMapper,
                          DrcSubTaskFullConfig fullConfig,
                          DrcDbConfigMapper drcDbConfigMapper,
                          DrcSubTaskFullSliceDetailMapper drcSubTaskFullSliceDetailMapper,
                          DrcSubTaskSchemaLogMapper drcSubTaskSchemaLogMapper,
                          Integer limit) {
        this.drcTaskMapper = drcTaskMapper;
        this.fullConfig = fullConfig;
        this.drcDbConfigMapper = drcDbConfigMapper;
        this.limit = limit;
        this.drcSubTaskFullConfigMapper = drcSubTaskFullConfigMapper;
        this.drcSubTaskFullSliceDetailMapper = drcSubTaskFullSliceDetailMapper;
        this.drcSubTaskSchemaLogMapper = drcSubTaskSchemaLogMapper;
        splitPool.allowCoreThreadTimeOut(true);
        startPool.allowCoreThreadTimeOut(true);
        this.consumer = consumer;
    }

    public void run() {
        try {
            run0();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 出错就回滚.
     */
    @Transactional(rollbackFor = Exception.class)
    public void run0() {
        String tableName = fullConfig.getTableName();
        Integer rangeSize = fullConfig.getRangeSizeConfig();
        Integer drcTaskId = fullConfig.getDrcTaskId();
        DrcTask drcTask = drcTaskMapper.selectByPrimaryKey(drcTaskId);
        String taskName = drcTask.getTaskName();

        Integer dbConfigId = fullConfig.getDbConfigId();
        String whereStatement = fullConfig.getWhereStatement();
        DrcDbConfig drcDbConfig = drcDbConfigMapper.selectByPrimaryKey(dbConfigId);

        TableSplitResolver tableSplitResolver = StringUtils.isEmpty(whereStatement)
                ? new TableSplitResolverSimple(channel)
                : new TableWithWhereSplitResolver(channel);


        // 异步分片
        splitPool.execute(() -> {
            try {
                Cost start = Cost.start();
                log.info("开始执行分片切分={}", taskName);
                tableSplitResolver.splitSlice(
                        fullConfig.getId(),
                        drcTaskId,
                        taskName,
                        tableName,
                        drcDbConfig.getUrl(),
                        drcDbConfig.getUsername(),
                        drcDbConfig.getPassword(),
                        drcDbConfig.getDatabaseName(),
                        rangeSize,
                        limit, whereStatement);

                DrcSubTaskSchemaLog schemaLog;

                // 拆分结束了, 需要更新 config 等相关表状态.
                while (true) {
                    schemaLog = drcSubTaskSchemaLogMapper.selectByParentTaskId(drcTaskId);
                    if (schemaLog == null) {
                        break;
                    }
                    Integer tableSplitFinish = schemaLog.getTableSplitFinish();
                    final int update = drcSubTaskSchemaLogMapper.updateTableSplitFinish(tableSplitFinish, tableSplitFinish + 1,
                            schemaLog.getId());
                    if (update > 0) {
                        break;
                    }
                }

                schemaLog = drcSubTaskSchemaLogMapper.selectByParentTaskId(drcTaskId);

                if (schemaLog != null && Objects.equals(schemaLog.getTableSplitFinish(), schemaLog.getTableTotal())) {
                    // 所有的表拆分完成.
                    DrcTask ddd = drcTaskMapper.selectByPrimaryKey(drcTask);
                    ddd.setState(TaskStateEnum.SPLIT_OVER.getCode());
                    drcTaskMapper.updateByPrimaryKeySelective(ddd);
                }

                log.info("结束执行分片切分={}, cost={}", taskName, start.end());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                AlarmUtil.pushAlarm2Admin("拆分出现问题, 请速速排查. taskName=" + taskName);
            }

        });

        // 异步保存到数据, 其他的线程会启动任务.
        startPool.execute(() -> {
            List<Slice> slices = Lists.newArrayList();

            while (true) {
                try {
                    Slice slice = channel.poll(1, TimeUnit.SECONDS);
                    if (slice == null) {
                        continue;
                    }
                    // 拆分结束.
                    if (slice == Slice.EMPTY) {
                        break;
                    }
                    insert(slice);
                    slices.add(slice);
                } catch (InterruptedException e) {
                    // ignore
                    log.error("线程被打断......");
                    break;
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

            // 分片结束.
            // 记录分片数量.
            drcSubTaskFullConfigMapper.updateSliceCount(0, slices.size(), fullConfig.getId());

            // 更新状态,拆分完毕.
            DrcSubTaskFullConfig model = drcSubTaskFullConfigMapper.selectByPrimaryKey(fullConfig.getId());
            model.setSplitState(DrcSubTaskFullConfig.SPLIT_STATE_OVER);
            drcSubTaskFullConfigMapper.updateByPrimaryKeySelective(model);
        });
    }

    /**
     * 插入分片配置.
     */
    public void insert(Slice data) {
        DrcSubTaskFullSliceDetail dbData = new DrcSubTaskFullSliceDetail();

        dbData.setParentId(data.getParentId());
        dbData.setSliceNumber(data.getSliceNumber());
        dbData.setSubTaskName(data.getSliceName());
        dbData.setSliceMinPk(data.getMinPkValue());
        dbData.setSliceMaxPk(data.getMaxPkValue());
        dbData.setRangeSize(data.getRangeSize());
        dbData.setIsLastSlice(data.isLastRange() ? IS_LAST_SLICE_TRUE : IS_LAST_SLICE_FALSE);
        dbData.setSliceCursor(data.getMinPkValue());
        dbData.setState(TaskStateEnum.INIT.code);
        dbData.setDrcSubTaskFullConfigId(data.getDrcSubTaskFullConfigId());
        dbData.setSlicePkName(data.getSlicePkName());

        // 插入分片配置.
        drcSubTaskFullSliceDetailMapper.insertSelective(dbData);

        if (consumer != null) {
            consumer.accept(null);
        }
    }


}
