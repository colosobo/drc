package com.timevale.drc.worker.service.task.mysql;

import com.timevale.drc.base.metrics.DefaultTaskMetrics;
import lombok.SneakyThrows;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class DefaultTaskMetricsTest {

    DefaultTaskMetrics defaultTaskMetrics = new DefaultTaskMetrics(DefaultTaskMetrics.class.getName());

    @Test
    public void currentQPSTest() {
        for (int i = 0; i < 15; i++) {
            new Thread(new Runnable() {
                @SneakyThrows
                @Override
                public void run() {
                    long start = System.currentTimeMillis();
                    while (true) {
                        Thread.sleep(92);
                        if (System.currentTimeMillis() - start >  TimeUnit.SECONDS.toMillis(15)) {
                            Thread.sleep(990);
                        }
                        if (System.currentTimeMillis() - start >  TimeUnit.SECONDS.toMillis(30)) {
                            return;
                        }
                        defaultTaskMetrics.stat();
                    }
                }
            }).start();
        }
        long start = System.currentTimeMillis();
        while (true) {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
            System.out.println(defaultTaskMetrics.currentQps());
            if (System.currentTimeMillis() - start > TimeUnit.SECONDS.toMillis(30)) {
                return;
            }
        }
    }

    @Test
    public void stat() {
    }
}
