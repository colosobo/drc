package com.timevale.drc.worker.service.task.mysql;

import com.timevale.drc.base.eventbus.DrcThreadPool;
import com.timevale.drc.base.eventbus.DrcThreadPoolExecutor;
import com.timevale.drc.base.serialize.JackSonUtil;
import com.timevale.drc.base.util.DateUtils;
import com.timevale.drc.base.util.DrcZkClient;
import com.timevale.drc.base.util.ZkPathConstant;
import com.timevale.drc.worker.service.task.Coordinator;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.IZkDataListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * zk 协调者.
 *
 * @author gwk_2
 * @date 2021/3/11 15:50
 */
@Component
@Slf4j
public class ZkCoordinator implements Coordinator {

    public static final String CONTENT_COORDINATOR_OVER = "coordinator_over";

    private DrcThreadPoolExecutor asyncPool = DrcThreadPool.createThreadPool("ZkCoordinator-Pool", 10, 128);

    @Autowired
    private DrcZkClient drcZkClient;

    @Override
    public void watch(String parentTaskName, Runnable callback) {
        log.info("监听协调事件: parentTaskName = {}", parentTaskName);
        String coordinatorPath = ZkPathConstant.getCoordinator(parentTaskName);
        Runner runner = new Runner(coordinatorPath, callback);
        drcZkClient.subscribeDataChanges(coordinatorPath, runner);
    }

    @Override
    public void unWatch(String parentTaskName) {
        String coordinatorPath = ZkPathConstant.getCoordinator(parentTaskName);
        drcZkClient.unsubscribeChildChanges(coordinatorPath, (parentPath, currentChilds) -> {
        });
    }

    @Override
    public void publish(String parentTaskName) {
        log.info("发布协调事件, parentTaskName ={}", parentTaskName);
        String coordinatorPath = ZkPathConstant.getCoordinator(parentTaskName);
        boolean exists = drcZkClient.exists(coordinatorPath);
        if (!exists) {
            drcZkClient.createPersistent(coordinatorPath, true);
        }
        ZkCoordinatorObj zkCoordinatorObj = new ZkCoordinatorObj(Coordinator.OVER_TIPS);
        drcZkClient.writeData(coordinatorPath, zkCoordinatorObj.toJson());
    }

    class Runner implements IZkDataListener {

        private String coordinatorPath;
        private Runnable callback;

        public Runner(String coordinatorPath, Runnable callback) {
            this.coordinatorPath = coordinatorPath;
            this.callback = callback;
        }

        @Override
        public void handleDataChange(String dataPath, Object data) {
            if (data == null) {
                return;
            }
            ZkCoordinatorObj zkCoordinatorObj = ZkCoordinatorObj.toObj(data.toString());
            if (zkCoordinatorObj == null) {
                return;
            }
            // 如果极端情况下, 全量通知的时候, 增量挂了, 就无法进行回放了, 此时, 可以用手动操作.
            if (dataPath.equals(coordinatorPath) && zkCoordinatorObj.content.equals(OVER_TIPS)) {
                log.info("触发协调事件. coordinatorPath = {}", coordinatorPath);
                asyncPool.execute(() -> {
                    try {
                        zkCoordinatorObj.setContent(CONTENT_COORDINATOR_OVER);
                        drcZkClient.writeData(coordinatorPath, zkCoordinatorObj);

                        callback.run();
                        // 删除这个数据
                        log.info("删除协调数据. coordinatorPath = {}", coordinatorPath);
                        drcZkClient.deleteRecursive(coordinatorPath);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                });
            }
        }

        @Override
        public void handleDataDeleted(String dataPath) throws Exception {

        }
    }

    @Data
    public static class ZkCoordinatorObj {
        String content;
        String time;

        public ZkCoordinatorObj() {
        }

        public ZkCoordinatorObj(String content) {
            this.content = content;
            this.time = DateUtils.format(new Date(), DateUtils.newFormat);
        }

        public String toJson() {
            return JackSonUtil.obj2String(this);
        }

        public static ZkCoordinatorObj toObj(String json) {
            return JackSonUtil.string2Obj(json, ZkCoordinatorObj.class);
        }
    }
}
