package com.timevale.drc.base;

import com.timevale.drc.base.log.TaskLog;

/**
 * @author gwk_2
 * @date 2021/1/28 23:19
 */
public abstract class BaseTask implements Task {

    protected volatile boolean running = false;

    @Override
    public void start() {
        running = true;
        TaskLog log = getLog();
        if (log != null) {
            log.info("[star task] = " + getName());
        }
    }

    @Override
    public void stop(String cause) {
        running = false;
        TaskLog log = getLog();
        if (log != null) {
            log.info("[stop task] = " + getName() + ", cause=" + cause);
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * 获取提取器.
     *
     * @return
     */
    public abstract Extract<?> getExtract();

    /**
     * 获取转换器.
     *
     * @return
     */
    public abstract Transform<?> getTransform();


    /**
     * 获取写入器.
     *
     * @return
     */
    public abstract Sink<?> getSink();

    public abstract TaskLog getLog();
}
