package com.timevale.drc.base.log;

public interface TaskLog {

    void info(Object message, Throwable t);

    void info(Object message);

    void warn(Object message);

    void warn(Object message, Throwable t);

    void error(Object message);

    void error(Object message, Throwable t);
}
