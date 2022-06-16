package com.timevale.drc.worker.service.task.mysql;

import com.google.gson.Gson;
import com.timevale.drc.base.eventbus.DrcThreadPool;
import com.timevale.drc.base.util.DateUtils;
import com.timevale.drc.worker.service.task.Coordinator;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author gwk_2
 * @date 2022/4/29 16:09
 */
@Slf4j
@Component("RedisCoordinator")
public class RedisCoordinator implements Coordinator {

    private static final String PREFIX = "DRC_WORKER_REDIS_COORDINATOR_";
    private static final Gson GSON = new Gson();

    private final ScheduledExecutorService scheduled = DrcThreadPool.newScheduledThreadPool(1, "RedisCoordinator");

    @Resource
    private RedissonClient redissonClient;

    private ConcurrentHashMap<String, Runnable> map = new ConcurrentHashMap<>();

    public RedisCoordinator() {
        run();
    }

    public RedisCoordinator(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void watch(String parentTaskName, Runnable callback) {
        if (StringUtils.isBlank(parentTaskName)) {
            throw new RuntimeException("parentTaskName can not be null.");
        }
        map.put(parentTaskName, callback);
    }

    @Override
    public void unWatch(String parentTaskName) {
        map.remove(parentTaskName);
    }

    @Override
    public void publish(String parentTaskName) {
        log.info("发布事件.....{}", parentTaskName);
        RBucket<String> bucket = redissonClient.getBucket(PREFIX + parentTaskName);
        Model model = new Model();
        model.setContent(OVER_TIPS);
        bucket.set(GSON.toJson(model));
    }


    private void run() {
        scheduled.scheduleAtFixedRate(() -> {
            for (String parentTaskName : map.keySet()) {
                final Runnable callback = map.get(parentTaskName);
                if (callback == null) {
                    continue;
                }
                try {
                    RBucket<String> bucket = redissonClient.getBucket(PREFIX + parentTaskName);
                    String json = bucket.get();
                    if (json == null) {
                        continue;
                    }

                    Model modelm = GSON.fromJson(json, Model.class);

                    if (modelm.getContent().equalsIgnoreCase(OVER_TIPS)) {
                        log.info(parentTaskName + " 收到通知, 开始执行回调.");
                        modelm = new Model();
                        modelm.setContent("bye bye " + DateUtils.format(new Date(), DateUtils.newFormat));
                        bucket.set(GSON.toJson(modelm));
                        callback.run();
                    }
                } catch (Exception e) {
                    // 忽略异常.
                    log.error(e.getMessage(), e);
                }
            }

        }, 1, 1, TimeUnit.SECONDS);
    }


    @Data
    static class Model {
        String content;
    }
}
