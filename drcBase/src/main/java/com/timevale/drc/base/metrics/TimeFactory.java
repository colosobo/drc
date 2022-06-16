package com.timevale.drc.base.metrics;

import java.util.concurrent.TimeUnit;

/**
 * 由于 System.currentTimeMillis() 消耗性能较高,而我们只需要一个大概时间即可,此为优化措施.
 */
public class TimeFactory {

    private static volatile long currentTimeMillis;

    static {
        currentTimeMillis = System.currentTimeMillis();
        Thread daemon = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    currentTimeMillis = System.currentTimeMillis();
                    try {
                        TimeUnit.MILLISECONDS.sleep(1);
                    } catch (Throwable e) {
                        // NO Thing
                    }
                }
            }
        });
        daemon.setDaemon(true);
        daemon.setName("DRC-Time-Factory-Daemon-Thread");
        daemon.start();
    }

    public static long currentTimeMillis() {
        return currentTimeMillis;
    }


}
