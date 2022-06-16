package com.timevale.drc.worker.service.task.mysql;

import com.timevale.drc.worker.deploy.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class ZkCoordinatorTest {

    @Autowired
    ZkCoordinator zkCoordinator;


    @Test
    public void watch() {

        final int[] a = {1};

        zkCoordinator.watch("testTask", new Runnable() {
            @Override
            public void run() {
                System.out.println("hello testTask");
                a[0] = 2;
            }
        });

        zkCoordinator.publish("testTask");
        while (a[0] == 1) {
            LockSupport.parkNanos(TimeUnit.SECONDS.toSeconds(1));
        }
        LockSupport.parkNanos(TimeUnit.SECONDS.toSeconds(3));
    }

    @Test
    public void publish() {
    }
}
