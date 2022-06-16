package com.timevale.drc.base.eventbus;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * @author gwk_2
 * @date 2021/8/18 16:08
 */
@Slf4j
public class DrcThreadPoolExecutor extends ThreadPoolExecutor {

    private final String name;
    public DrcThreadPoolExecutor(String name, int corePoolSize, int maximumPoolSize,
                                 long keepAliveTime, TimeUnit unit,
                                 BlockingQueue<Runnable> workQueue,
                                 ThreadFactory threadFactory,
                                 RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        this.name = name;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        try {
            // 等待老的任务运行完毕. 防止丢失任务.
            int count = 0;
            while (!awaitTermination(1, TimeUnit.SECONDS)) {
                log.info("wait shutdown over {} .....", count++);
            }
            log.info("shutdown success.....count={}, name={}", count, name);
        } catch (Exception e) {
            log.error("shutdown fail, e:", e);
        }
    }
}
