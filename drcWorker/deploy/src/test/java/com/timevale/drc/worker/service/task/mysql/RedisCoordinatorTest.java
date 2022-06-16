package com.timevale.drc.worker.service.task.mysql;

import com.timevale.drc.base.rpc.HostNameUtil;
import com.timevale.drc.worker.service.task.Coordinator;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class RedisCoordinatorTest {

    @Test
    public void watch() throws InterruptedException {

        Config config = new Config();

        if (HostNameUtil.getIp().startsWith("192")) {
            config.useSingleServer()
                    .setDnsMonitoringInterval(-1)
                    .setTimeout(3000)
                    // redisson 无法解析域名.
                    .setAddress("redis://" + "localhost" + ":6379");
        } else {
            return;
        }


        RedissonClient redisson = Redisson.create(config);

        Coordinator coordinator = new RedisCoordinator(redisson);

        CountDownLatch countDownLatch = new CountDownLatch(1);

        coordinator.watch("test", new Runnable() {
            @Override
            public void run() {
                System.out.println("callback");
                countDownLatch.countDown();
            }
        });
        coordinator.watch("test", new Runnable() {
            @Override
            public void run() {
                System.out.println("callback");
                countDownLatch.countDown();
            }
        });


        LockSupport.parkNanos(TimeUnit.SECONDS.toSeconds(1));

        coordinator.publish("test");

        countDownLatch.await();
    }

    @Test
    public void publish() {
    }
}
