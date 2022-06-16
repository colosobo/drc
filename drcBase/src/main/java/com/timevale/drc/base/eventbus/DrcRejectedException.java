package com.timevale.drc.base.eventbus;

/**
 * @author gwk_2
 * @date 2022/1/11 13:53
 */
public class DrcRejectedException extends RuntimeException{

    public DrcRejectedException() {
    }

    public DrcRejectedException(String message) {
        super(message);
    }

    public DrcRejectedException(String message, Throwable cause) {
        super(message, cause);
    }

    public DrcRejectedException(Throwable cause) {
        super(cause);
    }

    public DrcRejectedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
