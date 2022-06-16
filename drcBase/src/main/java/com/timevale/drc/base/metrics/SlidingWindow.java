package com.timevale.drc.base.metrics;

import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

/**
 * (滑动窗口).例如数组长度 60, 每个桶表示一秒内的统计信息.
 *
 * @author 玄灭
 * @date 2018/11/23-下午9:31
 */
@Getter
public class SlidingWindow implements Serializable {
    /**
     * 窗口长度(单个时间长度) {} 毫秒
     */
    private final int oneWindowsLengthInMs;
    /**
     * 桶的数量
     */
    private final int bucketCount;

    /** 总时间窗口(毫秒). */
    private final int totalWindowLengthInMs;

    /** 总窗口的长度(秒) */
    private final int totalWindowsTimeInSec;
    /**
     * 滑动数组
     */
    private final AtomicReferenceArray<MetricsBucket> slidingArray;

    private final ReentrantLock lock = new ReentrantLock();

    public SlidingWindow(int totalWindowLengthInMs, int oneWindowLengthInMs) {
        if (totalWindowLengthInMs < 1000) {
            throw new RuntimeException("总时间窗口必须大于一秒.");
        }
        if ((totalWindowLengthInMs % oneWindowLengthInMs) != 0) {
            throw new RuntimeException("单个时间窗口必须能 1000 被总时间窗口整除.");
        }
        this.oneWindowsLengthInMs = oneWindowLengthInMs;
        this.totalWindowLengthInMs = totalWindowLengthInMs;

        this.bucketCount = this.totalWindowLengthInMs / this.oneWindowsLengthInMs ;
        this.totalWindowsTimeInSec = this.totalWindowLengthInMs / 1000;
        this.slidingArray = new AtomicReferenceArray<>(bucketCount);
    }

    /**
     * 获取当前秒时间的桶.
     */
    public MetricsBucket getCurrentWindow() {
        // 毫秒
        long time = TimeFactory.currentTimeMillis();
        // 获取桶的下标(循环数组)
        long s = time / oneWindowsLengthInMs;
        int idx = (int) (s % bucketCount);
        // 让该时间从零开始计数
        time = time - time % oneWindowsLengthInMs;

        for (; ; ) {
            MetricsBucket old = slidingArray.get(idx);
            if (old == null) {
                old = new MetricsBucket(time);
                if (slidingArray.compareAndSet(idx, null, old)) {
                    return old;
                } else {
                    Thread.yield();
                }
            } else if (time == old.getStartTime()) {
                // 就是这个
                return old;
            } else if (time > old.getStartTime()) {
                if (lock.tryLock()) {
                    try {
                        // 重置
                        return old.reset(time);
                    } finally {
                        lock.unlock();
                    }
                } else {
                    Thread.yield();
                }
            } else if (time < old.getStartTime()) {
                // 通常不会发生.
                throw new RuntimeException();
            }
        }
    }

    public List<MetricsBucket> values() {
        List<MetricsBucket> list = new ArrayList<>();
        for (int i = 0; i < slidingArray.length(); i++) {
            MetricsBucket mb = slidingArray.get(i);
            // 必须在当前时间的窗口中
            if (mb == null || TimeFactory.currentTimeMillis() - totalWindowLengthInMs > mb.getStartTime()) {
                continue;
            }
            list.add(mb);
        }
        return list;
    }

    public void reset() {
        for (int i = 0; i < slidingArray.length(); i++) {
            MetricsBucket mb = slidingArray.get(i);
            if (mb != null) {
                mb.reset();
            }
        }
    }

    public List<MetricsBucket> getSlidingArray() {
        return values();
    }

    @Override
    public String toString() {
        return "SlidingWindow{\r\n" +
                "oneWindowsLengthInMs=" + oneWindowsLengthInMs +
                ", bucketCount=" + bucketCount +
                ", totalWindowsTimeInMs=" + totalWindowLengthInMs +
                ", slidingArray=" + slidingArray +
                '}';
    }
}
