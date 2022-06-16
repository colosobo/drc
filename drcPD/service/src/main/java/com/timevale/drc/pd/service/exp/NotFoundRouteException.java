package com.timevale.drc.pd.service.exp;

/**
 * @author gwk_2
 * @date 2021/3/27 02:24
 */
public class NotFoundRouteException extends RuntimeException{

    public NotFoundRouteException() {
    }

    public NotFoundRouteException(String message) {
        super(message);
    }

    public NotFoundRouteException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundRouteException(Throwable cause) {
        super(cause);
    }

    public NotFoundRouteException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
