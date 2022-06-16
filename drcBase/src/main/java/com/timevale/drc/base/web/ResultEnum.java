package com.timevale.drc.base.web;

import lombok.Getter;

/**
 * @author gwk_2
 * @date 2022/1/17 22:10
 */
@Getter
public enum  ResultEnum {
    /** e */
    SUCCESS("SUCCESS", "操作成功", 0),
    SYSTEM_FAILURE("SYSTEM_FAILURE", "系统错误", 1000000),
    SYSTEM_ERROR("SYSTEM_ERROR", "系统异常", 1000001),
    CONNECTION_ERROR("CONNECTION_ERROR", "链路异常", 1000002),
    CONNECTION_TIME_OUT("CONNECTION_TIME_OUT", "系统超时，请稍后再试!", 1000003),
    BIZ_ERROR("BIZ_ERROR", "业务异常", 1000004),
    ILLEGAL_ARGUMENT("ILLEGAL_ARGUMENT", "非法参数", 1000005),
    ILLEGAL_OPERATION("ILLEGAL_OPERATION", "非法操作", 1000006),
    REPEATED_OPERATION("REPEATED_OPERATION", "重复操作", 1000007),
    FRAMEWORK_ERROR("FRAMEWORK_ERROR", "框架异常", 1000008);

    private String code;
    private String message;
    private int errCode;

    ResultEnum(String code, String message, int errCode) {
        this.code = code;
        this.message = message;
        this.errCode = errCode;
    }
}
