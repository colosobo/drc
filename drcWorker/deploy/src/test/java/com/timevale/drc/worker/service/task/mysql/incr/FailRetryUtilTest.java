package com.timevale.drc.worker.service.task.mysql.incr;

import org.junit.Test;

public class FailRetryUtilTest {

    static int a = 0;

    @Test
    public void failRetry() {
        FailRetryUtil.failRetry(null, new Runnable() {
            @Override
            public void run() {
                if (a < 2) {
                    a++;
                    throw new RuntimeException();
                }
                System.out.println("success");
            }
        }, new FailRetryUtil.FailCallBack() {
            @Override
            public void callback(Exception e) {
                e.printStackTrace();
            }
        });
    }
}
