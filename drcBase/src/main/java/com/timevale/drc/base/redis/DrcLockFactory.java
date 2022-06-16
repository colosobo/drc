package com.timevale.drc.base.redis;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author gwk_2
 * @date 2022/1/17 18:32
 */
@Component
public class DrcLockFactory {

    @Autowired
    private RedissonClient redissonClient;

    public DrcLock getLock(String name) {
        return new DrcLock(name, this.redissonClient);
    }

}
