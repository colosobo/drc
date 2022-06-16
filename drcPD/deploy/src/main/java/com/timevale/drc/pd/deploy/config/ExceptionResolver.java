package com.timevale.drc.pd.deploy.config;

import com.timevale.drc.base.web.BaseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author gwk_2
 * @date 2021/3/17 17:30
 */
@ControllerAdvice
@Slf4j
public class ExceptionResolver {

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public BaseResult exceptionHandler(Exception e) {
        log.warn(e.getMessage(), e);
        return new BaseResult(false, e.getMessage());
    }
}
