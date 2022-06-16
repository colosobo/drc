package com.timevale.drc.base.log;

import com.timevale.drc.base.util.LogUtil;

public class ApacheTaskLog implements TaskLog {

    private final org.apache.log4j.Logger delegate;

    public ApacheTaskLog(String name) {
        this.delegate = LogUtil.getLogger(name);
    }

    public void info(Object message, Throwable t) {
        delegate.info(message, t);
    }

    public void info(Object message) {
        delegate.info(message);
    }

    public void warn(Object message) {
        delegate.warn(message);
    }

    public void warn(Object message, Throwable t) {
        delegate.warn(message, t);
    }

    public void error(Object message) {
        delegate.error(message);
    }

    public void error(Object message, Throwable t) {
        delegate.error(message, t);
    }

}
