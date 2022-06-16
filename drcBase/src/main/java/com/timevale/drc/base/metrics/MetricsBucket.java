package com.timevale.drc.base.metrics;

import java.util.concurrent.atomic.LongAdder;

/**
 *
 * 指标桶, 表示一段时间的数据.
 *
 * @author 玄灭
 * @date 2018/11/23-下午9:27
 */
public class MetricsBucket {

    /** 请求总数量 */
    private LongAdder total = new LongAdder();
    /** 异常数量 */
    private LongAdder exception = new LongAdder();
    /** 此桶的计数开始时间 */
    private long startTime;
    /** 异常数量 */
    private LongAdder byteCount = new LongAdder();

    public MetricsBucket(long startTime) {
        this.startTime = startTime;
    }

    public long getException() {
        return exception.sum();
    }

    public void addTotal() {
        total.add(1L);
    }

    public void addTotal(long count) {
        total.add(count);
    }

    public void addByteCount(long count) {
        byteCount.add(count);
    }

    public long getTotal() {
        return this.total.sum();
    }

    public long getByteCount() {
        return byteCount.sum();
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public MetricsBucket reset(long time) {
        total.reset();
        exception.reset();
        byteCount.reset();
        startTime = time;
        return this;
    }

    public MetricsBucket reset() {
        total.reset();
        exception.reset();
        byteCount.reset();
        return this;
    }

    @Override
    public String toString() {
        return "\r\nMetricsBucket{" +
            "total=" + total +
            ", exception=" + exception +
            ", startTime=" + startTime +
            '}';
    }
}
