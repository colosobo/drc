package com.timevale.drc.base.util;

import lombok.Data;
import lombok.ToString;

/**
 * @author gwk_2
 * @date 2021/3/9 19:44
 */
@Data
@ToString
public class CheckTableExistResult {

    boolean exists;

    String msg;

    public CheckTableExistResult(boolean exists, String msg) {
        this.exists = exists;
        this.msg = msg;
    }

    public static CheckTableExistResult buildSuccess() {
        return new CheckTableExistResult(true, "表存在.");
    }

    public static CheckTableExistResult buildError(String msg) {
        return new CheckTableExistResult(false, msg);
    }
}
