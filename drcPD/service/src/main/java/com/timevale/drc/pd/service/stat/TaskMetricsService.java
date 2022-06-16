package com.timevale.drc.pd.service.stat;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TaskMetricsService {

    private static final ConcurrentHashMap<String, LinkedList<DataModel>> CACHE = new ConcurrentHashMap<>();

    /**
     */
    public static void put(String name, Integer qps) {
        LinkedList<DataModel> qpsList = CACHE.get(name);
        if (qpsList == null) {
            qpsList = new LinkedList<>();
        }

        synchronized (qpsList) {

            while (qpsList.size() >= 3) {
                qpsList.removeFirst();
            }

            qpsList.addLast(new DataModel(qps, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));
        }
    }

    public int getTotalQPS() {
        int count = 0;
        for (Map.Entry<String, LinkedList<DataModel>> e : CACHE.entrySet()) {
            count += getQPS(e.getKey());
        }
        return count;
    }

    public int getQPS(String taskName) {
        LinkedList<DataModel> linkedList = CACHE.get(taskName);
        if (linkedList == null) {
            linkedList = new LinkedList<>();
            CACHE.put(taskName, linkedList);
            return 0;
        }

        int qps = 0;
        if (linkedList.size() == 0) {
            return 0;
        }
        int size = 0;
        for (DataModel i : linkedList) {
            if (i.timeInSec + 3 < TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())) {
                continue;
            }
            size += 1;
            qps += i.qps;
        }

        if (size == 0) {
            return 0;
        }
        return qps / size;
    }

    static class DataModel {
        int qps;
        long timeInSec;

        public DataModel(int qps, long timeInSec) {
            this.qps = qps;
            this.timeInSec = timeInSec;
        }
    }

}

