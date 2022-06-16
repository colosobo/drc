package com.timevale.drc.base;

import lombok.Data;
import lombok.ToString;

/**
 * 任务统计指标.
 */
public interface TaskMetrics {

    /**
     * 当前 QPS
     * @return qps
     */
    int currentQps();

    @Data
    @ToString
    class SimpleTaskMetrics implements TaskMetrics {
        int currentQps;
        // for json
        public SimpleTaskMetrics() {
        }

        public SimpleTaskMetrics(int currentQps) {
            this.currentQps = currentQps;
        }

        @Override
        public int currentQps() {
            return currentQps;
        }

    }

    class Factory {
        public static TaskMetrics create(int currentQps) {
            return new SimpleTaskMetrics(currentQps);
        }
    }
}
