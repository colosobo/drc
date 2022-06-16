package com.timevale.drc.worker.service.task.mysql;

/**
 * @author gwk_2
 * @date 2022/4/30 21:06
 * @description
 */
public interface Replay {

    /**
     * 开始回放
     */
    void replay();

    /**
     * 停止回放
     */
    void stop();

    /**
     * 获取回放差值
     * @return -1 表示不准确, 0 表示无差值. n>0 表示有 n 条没有回放完.
     */
    long getDiff();
}
