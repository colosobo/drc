package com.timevale.drc.pd.service;

import com.timevale.drc.base.util.ZkPathConstant;

import java.util.List;

/**
 * @author gwk_2
 * @date 2021/1/28 21:47
 * @description
 */
public interface LoadBalancer<W, T> {

    String DRC_PD_RE_BALANCE_LOCK_KEY = ZkPathConstant.GROUP + "_" + "DRC_PD_RE_BALANCE_LOCK_KEY";

    /**
     * 对 task 进行 reBalance, 可能是 worker 增加了, 也可能是 worker 减少了.
     *
     * @param workList 健康的 worker
     * @param taskList 总的 task 数量.
     */
    void reBalance(List<W> workList, List<T> taskList, String cause);

    /**
     * 手动故障转移, 将某个 task 转移到 指定的 worker 中.
     * @param worker
     * @param task
     */
    void failover(W worker, T task);

}
