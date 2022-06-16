package com.timevale.drc.base.redis;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * @author gwk_2
 * @date 2022/1/8 03:52
 */
public class DrcRedisson {

    private static RedissonClient client;

    public static void setRedisson(RedissonClient redissonClient) {
        DrcRedisson.client = redissonClient;
    }

    public static <T> T get(String key) {
        RBucket<T> bucket = client.getBucket(key);
        return bucket.get();
    }

    public static <T> void set(String key, T value) {
        if (value == null) {
            return;
        }
        client.getBucket(key).set(value);
    }

    public static void delete(String key) {
        RBucket<Object> bucket = client.getBucket(key);
        bucket.delete();
    }

    public static <T> void set(String key, T val, long timeout, TimeUnit unit) {
        client.getBucket(key).set(val, timeout, unit);
    }

    public static RedissonClient getClient() {
        return client;
    }
}
