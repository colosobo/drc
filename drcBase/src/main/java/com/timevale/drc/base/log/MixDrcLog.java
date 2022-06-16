package com.timevale.drc.base.log;

import org.slf4j.Logger;

public class MixDrcLog implements DrcLog {

    private Logger slfLog;
    private TaskLog taskLog;

    public MixDrcLog(Logger slfLog, TaskLog taskLog) {
        this.slfLog = slfLog;
        this.taskLog = taskLog;
    }

    @Override
    public void info(Object message, Throwable t) {
        slfLog.info(message.toString(), t);
        taskLog.info(message, t);
    }

    @Override
    public void info(Object message) {
        slfLog.info(message.toString());
        taskLog.info(message);
    }

    @Override
    public void warn(Object message) {
        slfLog.warn(message.toString());
        taskLog.warn(message);
    }

    @Override
    public void warn(Object message, Throwable t) {
        slfLog.warn(message.toString(), t);
        taskLog.warn(message, t);
    }

    @Override
    public void error(Object message) {
        slfLog.error(message.toString());
        taskLog.error(message);
    }

    @Override
    public void error(Object message, Throwable t) {
        taskLog.error(message, t);
    }
}
