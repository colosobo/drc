package com.timevale.drc.base.eventbus;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.*;

/**
 * @author gwk_2
 * @date 2020/11/25 20:04
 */
public class DrcThreadPool {

    private static final int CPU = Runtime.getRuntime().availableProcessors();
    private static final long KEEP_ALIVE = 60;
    private static final TimeUnit UNIT = TimeUnit.SECONDS;
    private static final int QUEUE_SIZE = 1024;

    public static DrcThreadPoolExecutor createThreadPoolWithZeroQueue(int core, String threadName) {
        return createThreadPool(threadName, core, 0);
    }


    public static DrcThreadPoolExecutor createThreadPool(String threadName) {
        return createThreadPool(threadName, CPU, QUEUE_SIZE);
    }

    public static DrcThreadPoolExecutor createThreadPool(String threadName, int core, int queueSize) {
        return new DrcThreadPoolExecutor(threadName,
                core,
                core * 2,
                KEEP_ALIVE,
                UNIT,
                queueSize == 0 ? new SynchronousQueue<>() : new LinkedBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder().setNameFormat(threadName + "-%d").build(),
                new DrcRejectedExecutionHandler());
    }

    public static ScheduledExecutorService newScheduledThreadPool(int core, String name) {
        return new ScheduledThreadPoolExecutor(core, new ThreadFactoryBuilder().setNameFormat(name + "-%d").build());
    }

}
