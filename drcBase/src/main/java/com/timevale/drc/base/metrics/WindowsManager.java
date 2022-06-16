package com.timevale.drc.base.metrics;

import com.timevale.drc.base.util.GlobalConfigUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 时间窗口生命周期管理器.
 *
 * @author 玄灭
 * @date 2018/11/23-下午10:28
 */
public class WindowsManager {

    private final Map<String /*unique*/, SlidingWindow> map = new ConcurrentHashMap<>();


    public static WindowsManager getInstance() {
        return StatisticManagerInner.INSTANCE;
    }

    private WindowsManager() {
    }

    private static class StatisticManagerInner {
        private static final WindowsManager INSTANCE = new WindowsManager();
    }


    /**
     * 处理请求(对总数 加一).
     *
     * @param unique
     */
    public void addTpsTotal(String unique) {
        SlidingWindow sw = getSlidingWindow(unique);
        MetricsBucket mb = sw.getCurrentWindow();
        mb.addTotal();
    }
    public void addTpsTotal(String unique, long count) {
        SlidingWindow sw = getSlidingWindow(unique);
        MetricsBucket mb = sw.getCurrentWindow();
        mb.addTotal(count);
    }

    public void addByteCount(String unique, long count) {
        SlidingWindow sw = getSlidingWindow(unique);
        MetricsBucket mb = sw.getCurrentWindow();
        mb.addByteCount(count);
    }

    /**
     * 获取时间窗口的平均值.
     *
     * @param unique
     * @return
     */
    public long getAvgInSeconds(String unique) {
        SlidingWindow sw = getSlidingWindow(unique);
        List<MetricsBucket> list = sw.values();
        long total = 0;
        for (MetricsBucket metricsBucket : list) {
            total += metricsBucket.getTotal();
        }
        return total / sw.getTotalWindowsTimeInSec();
    }

    public long getAvgByteCountInSeconds(String unique) {
        SlidingWindow sw = getSlidingWindow(unique);
        List<MetricsBucket> list = sw.values();
        long total = 0;
        for (MetricsBucket metricsBucket : list) {
            total += metricsBucket.getByteCount();
        }
        return total / sw.getTotalWindowsTimeInSec();
    }

    private SlidingWindow getSlidingWindow(String unique) {
        SlidingWindow sw = map.get(unique);
        if (sw == null) {
            sw = new SlidingWindow(
                    GlobalConfigUtil.getTotalWindowLengthInMs(),
                    GlobalConfigUtil.getOneWindowLengthInMs());

            SlidingWindow old = map.putIfAbsent(unique, sw);
            if (old != null) {
                sw = old;
            }
        }
        return sw;
    }

}
