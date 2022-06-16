package com.timevale.drc.base.metrics;

import com.timevale.drc.base.TaskMetrics;

/**
 * 默认的 QPS 统计实现.
 */
public class DefaultTaskMetrics implements TaskMetrics {

    private final WindowsManager windowsManager = WindowsManager.getInstance();
    private final String name;

    public DefaultTaskMetrics(String name) {
        this.name = name;
    }

    @Override
    public int currentQps() {
        long total = windowsManager.getAvgInSeconds(name);
        // 防止不太可能出现的溢出异常.
        if (total > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.toIntExact(total);
    }

    public void stat() {
        windowsManager.addTpsTotal(name);
    }

    public void stat(long count) {
        windowsManager.addTpsTotal(name, count);
    }

}
