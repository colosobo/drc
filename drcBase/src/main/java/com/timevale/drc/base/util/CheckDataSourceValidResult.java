package com.timevale.drc.base.util;

import lombok.Data;
import lombok.ToString;

/**
 * @author gwk_2
 * @date 2021/3/9 19:44
 */
@Data
@ToString
public class CheckDataSourceValidResult {

    boolean valid;

    String msg;

    public CheckDataSourceValidResult(boolean valid, String msg) {
        this.valid = valid;
        this.msg = msg;
    }

    public static CheckDataSourceValidResult buildSuccess() {
        return new CheckDataSourceValidResult(true, "数据源有效.");
    }

    public static CheckDataSourceValidResult buildError(String msg) {
        return new CheckDataSourceValidResult(false, msg);
    }
}
