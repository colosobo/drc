package com.timevale.drc.base.eventbus;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 扩展拒绝策略.
 */
@Slf4j
public class DrcRejectedExecutionHandler implements RejectedExecutionHandler {

    private int count;

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

        String tip = String.format("Task has been reject because of threadPool exhausted!" +
                        " pool:%s, active:%s, queue:%s, taskCount: %s",
                executor.getPoolSize(),
                executor.getActiveCount(),
                executor.getQueue().size(),
                executor.getTaskCount());
        if (++count % 10 == 0) {
            log.warn(tip);
        }
        throw new DrcRejectedException(tip);
    }
}
