package com.timevale.drc.worker.service.task.mysql.full.event;

import com.timevale.drc.base.TaskStateEnum;
import com.timevale.drc.base.TaskTypeEnum;
import com.timevale.drc.base.dao.DrcSubTaskFullConfigMapper;
import com.timevale.drc.base.dao.DrcSubTaskFullSliceDetailMapper;
import com.timevale.drc.base.dao.DrcSubTaskSchemaLogMapper;
import com.timevale.drc.base.dao.DrcTaskMapper;
import com.timevale.drc.base.eventbus.Subscriber;
import com.timevale.drc.base.model.DrcSubTaskFullConfig;
import com.timevale.drc.base.model.DrcSubTaskFullSliceDetail;
import com.timevale.drc.base.model.DrcSubTaskSchemaLog;
import com.timevale.drc.base.model.DrcTask;
import com.timevale.drc.worker.service.task.Coordinator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 全量结束, 需要做一些事情.
 *
 * @author gwk_2
 * @date 2021/3/10 11:08
 */
@Component
@Slf4j
public class FullMySqlExtractOverSubscriber extends Subscriber<FullMySqlExtractOverEvent> {

    @Autowired
    private DrcSubTaskFullConfigMapper drcSubTaskFullConfigMapper;
    @Resource(name = "RedisCoordinator")
    private Coordinator coordinator;
    @Autowired
    private DrcTaskMapper drcTaskMapper;
    @Autowired
    private DrcSubTaskSchemaLogMapper subTaskSchemaLogMapper;
    @Autowired
    private DrcSubTaskFullSliceDetailMapper drcSubTaskFullSliceDetailMapper;

    public FullMySqlExtractOverSubscriber() {
        super(FullMySqlExtractOverEvent.class);
    }

    @Override
    public boolean isSync() {
        return true;
    }

    @Override
    public void onEvent(FullMySqlExtractOverEvent event) {
        DrcSubTaskFullSliceDetail data = event.data();

        Integer fullConfigId = data.getDrcSubTaskFullConfigId();

        int retryCount = 1;
        while (true) {
            // 乐观锁并发更新失败，重试次数过多时
            if ((retryCount % 10) == 0) {
                log.warn("updateSliceCount 重试次数过多, 请关注..... fullConfigId {}", fullConfigId);
            }
            if (retryCount > 100) {
                log.error("updateSliceCount 重试次数超过 100 次,停止更新,  请关注..... fullConfigId = {}", fullConfigId);
                break;
            }
            DrcSubTaskFullConfig drcSubTaskFullConfig = drcSubTaskFullConfigMapper.selectByPrimaryKey(fullConfigId);
            Integer finishSliceCount = drcSubTaskFullConfig.getFinishSliceCount();
            int newFinishSliceCount;
            if (finishSliceCount == null) {
                newFinishSliceCount = 1;
            } else {
                newFinishSliceCount = finishSliceCount + 1;
            }
            //防止并发(基于oldSliceCount版本的乐观锁)
            int updateSliceCountResult = drcSubTaskFullConfigMapper.updateFinishSliceCount(finishSliceCount, newFinishSliceCount, drcSubTaskFullConfig.getId());
            if (updateSliceCountResult > 0) {
                break;
            }
            retryCount++;
        }

        DrcSubTaskFullConfig drcSubTaskFullConfig = drcSubTaskFullConfigMapper.selectByPrimaryKey(fullConfigId);

        Integer parentId = drcSubTaskFullConfig.getDrcTaskId();
        DrcTask drcTask = drcTaskMapper.selectByPrimaryKey(parentId);

        if (drcTask.getTaskType() == TaskTypeEnum.MYSQL_DATABASE_MIX_TASK.code) {
            // 一个库级别全量同步任务，所涉及的每一张表都会根据数据量大小被分割为一至多个subTask
            // 库级别全量同步任务，其中一个分片任务同步完成，更新父任务状态
            dataBaseSync(drcSubTaskFullConfig, parentId, drcTask);
        } else if (drcTask.getTaskType() == TaskTypeEnum.MYSQL_MIX_TASK.code) {
            // 一个表级别全量同步任务，根据数据量可以被分片为一至多个subTask
            // 表级别全量同步任务，其中一个分片任务同步完成
            // 如果完成的数量和分片数量一致，说明整个父任务都完成了, 发布父task结束事件
            if (drcSubTaskFullConfig.getFinishSliceCount() >= (drcSubTaskFullConfig.getSliceCount())) {
                coordinator.publish(drcTask.getTaskName());
            }
        }

    }

    private void dataBaseSync(DrcSubTaskFullConfig drcSubTaskFullConfig, Integer parentId, DrcTask drcTask) {
        // 如果这张表同步结束了, 那就更新 drc_sub_task_schema_log 的记录, 并加 1.
        if (drcSubTaskFullConfig.getFinishSliceCount() <= (drcSubTaskFullConfig.getSliceCount())) {
            int retryCount = 1;
            while (true) {
                DrcSubTaskSchemaLog drcSubTaskSchemaLog = subTaskSchemaLogMapper.selectByParentTaskId(parentId);
                if ((retryCount % 10) == 0) {
                    log.warn("updateTableFinish 重试次数过多, 请关注..... TaskName {}", drcTask.getTaskName());
                }
                if (retryCount > 100) {
                    log.error("updateTableFinish 重试次数超过 100 次,停止更新,  请关注..... TaskName = {}", drcTask.getTaskName());
                    break;
                }

                Integer splitFinish = drcSubTaskSchemaLog.getSplitFinish();
                // 库级别同步任务总，其中一个分片任务完成了，分片完成数+1
                // 防止并发(基于oldSplitFinish版本的乐观锁)
                int update = subTaskSchemaLogMapper.updateTableFinish(splitFinish, splitFinish + 1, drcSubTaskSchemaLog.getId());
                if (update > 0) {
                    break;
                }
                retryCount++;
            }

            DrcSubTaskSchemaLog drcSubTaskSchemaLog = subTaskSchemaLogMapper.selectByParentTaskId(parentId);

            final List<DrcSubTaskFullSliceDetail> list = drcSubTaskFullSliceDetailMapper.selectByParentId(parentId);
            if (list == null) {
                return;
            }

            // 分片任务完成数和实际的分片任务数一致，说明整个库同步结束了
            if (list.size() == (drcSubTaskSchemaLog.getSplitFinish())) {

                DrcTask dd = drcTaskMapper.selectByPrimaryKey(parentId);
                // 拆分结束了才行.
                if (dd.getState() == TaskStateEnum.SPLIT_OVER.code) {
                    coordinator.publish(drcTask.getTaskName());
                }

            }
        }
    }
}
