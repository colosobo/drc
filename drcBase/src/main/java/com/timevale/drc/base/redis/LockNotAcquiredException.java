package com.timevale.drc.base.redis;

public class LockNotAcquiredException extends RuntimeException {

    public LockNotAcquiredException() {
    }

    public LockNotAcquiredException(String message) {
        super(message);
    }

    public LockNotAcquiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockNotAcquiredException(Throwable cause) {
        super(cause);
    }

    public LockNotAcquiredException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
