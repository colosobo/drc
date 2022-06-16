package com.timevale.drc.worker.service.exp;

public class NotFoundTaskException extends RuntimeException {

    public NotFoundTaskException() {
    }

    public NotFoundTaskException(String message) {
        super(message);
    }

    public NotFoundTaskException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundTaskException(Throwable cause) {
        super(cause);
    }

    public NotFoundTaskException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
