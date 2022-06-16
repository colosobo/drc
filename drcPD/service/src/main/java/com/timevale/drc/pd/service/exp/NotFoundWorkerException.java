package com.timevale.drc.pd.service.exp;

/**
 * @author gwk_2
 * @date 2022/4/27 18:31
 */
public class NotFoundWorkerException extends RuntimeException {

    public NotFoundWorkerException() {
    }

    public NotFoundWorkerException(String message) {
        super(message);
    }

    public NotFoundWorkerException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundWorkerException(Throwable cause) {
        super(cause);
    }

    public NotFoundWorkerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
