package com.timevale.drc.base.web;

import lombok.Data;

/**
 * @author gwk_2
 * @date 2022/1/17 22:09
 */
@Data
public class BaseResult {

    protected boolean success = false;
    protected String code;
    protected String message;
    protected int errCode;


    public BaseResult() {
        this.success = true;
        this.code = ResultEnum.SUCCESS.getCode();
        this.message = ResultEnum.SUCCESS.getMessage();
        this.errCode = ResultEnum.SUCCESS.getErrCode();
    }

    public BaseResult(boolean success) {
        this.code = ResultEnum.SYSTEM_ERROR.getCode();
        this.message = ResultEnum.SYSTEM_ERROR.getMessage();
        this.errCode = ResultEnum.SYSTEM_ERROR.getErrCode();
        if (success) {
            this.success = true;
            this.code = ResultEnum.SUCCESS.getCode();
            this.message = ResultEnum.SUCCESS.getMessage();
            this.errCode = ResultEnum.SUCCESS.getErrCode();
        }
    }

    public BaseResult(boolean success, String msg) {
        this.code = ResultEnum.SYSTEM_ERROR.getCode();
        this.message = ResultEnum.SYSTEM_ERROR.getMessage();
        this.errCode = ResultEnum.SYSTEM_ERROR.getErrCode();
        this.message = msg;
        if (success) {
            this.success = true;
            this.code = ResultEnum.SUCCESS.getCode();
            this.errCode = ResultEnum.SUCCESS.getErrCode();
        }
    }

}
