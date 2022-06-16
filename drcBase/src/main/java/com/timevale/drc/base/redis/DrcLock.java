package com.timevale.drc.base.redis;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * @author gwk_2
 * @date 2022/1/17 18:33
 */
@Slf4j
public class DrcLock {

    private RLock rLock;
    private final String lockName;

    public DrcLock(String lockName, RedissonClient redissonClient) {
        this.lockName = lockName;
        // for test
        if (redissonClient != null) {
            this.rLock = redissonClient.getLock("__drc_lock__" + lockName);
        }
    }

    public <T> T lockAndProtect(int waitTimeInSec, Callable<T> callable) {
        try {
            boolean isAcquired = this.tryLock(waitTimeInSec, TimeUnit.SECONDS);
            if (!isAcquired) {
                throw new LockNotAcquiredException("获取锁失败！lockName: " + this.getLockName() + ", realLockName: " + this.lockName);
            }
        } catch (Exception e) {
            throw new LockNotAcquiredException(e);
        }

        T result;
        try {
            result = callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            this.unlock();
        }

        return result;
    }

    public void lockAndProtect(int waitTimeInSec, Runnable callable) {
        try {
            boolean isAcquired = this.tryLock(waitTimeInSec, TimeUnit.SECONDS);
            if (!isAcquired) {
                throw new LockNotAcquiredException("获取锁失败. lockName: " + this.getLockName() + ", realLockName: " + this.lockName);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            callable.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            this.unlock();
        }
    }

    public String getLockName() {
        return this.lockName;
    }

    public boolean tryLock(int waitTime, TimeUnit unit) {
        try {
            return this.rLock.tryLock(waitTime, unit);
        } catch (Throwable t) {
            log.error("RLock#tryLock尝试获取锁lockName: {} 异常！", this.lockName, t);
        }

        return false;
    }

    public void unlock() {
        try {
            this.rLock.unlock();
        } catch (IllegalMonitorStateException var2) {
            log.error("RLock#unlock尝试解锁他人的锁！持有的锁lockName: {} 可能已失效！", this.lockName, var2);
        } catch (Exception var3) {
            log.error("RLock#unlock解锁lockName: {} 异常！", this.lockName, var3);
        }

    }

}
