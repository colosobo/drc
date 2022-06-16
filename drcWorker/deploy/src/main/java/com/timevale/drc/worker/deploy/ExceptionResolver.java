package com.timevale.drc.worker.deploy;

import com.timevale.drc.base.web.BaseResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * @author gwk_2
 * @date 2021/3/17 17:30
 */
@ControllerAdvice
public class ExceptionResolver {

    @ExceptionHandler(value =Exception.class)
    @ResponseBody
    public BaseResult exceptionHandler(Exception e){
        if (e instanceof java.lang.reflect.UndeclaredThrowableException) {
            UndeclaredThrowableException exception = (UndeclaredThrowableException) e;
            Throwable cause = exception.getCause();
            if (cause instanceof java.lang.reflect.InvocationTargetException) {
                java.lang.reflect.InvocationTargetException invocationTargetException = (InvocationTargetException) cause;
                Throwable targetException = invocationTargetException.getTargetException();
                String msg = targetException.getClass().getName() + ":" + targetException.getMessage();
                throw new RuntimeException(msg);
            }
        }
        String msg = e.getMessage();
        throw new RuntimeException(msg);
    }
}
