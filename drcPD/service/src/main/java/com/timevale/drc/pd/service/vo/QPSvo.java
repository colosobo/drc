package com.timevale.drc.pd.service.vo;

import lombok.Data;

/**
 * @author gwk_2
 * @date 2021/4/22 22:38
 */
@Data
public class QPSvo {

    int qps;

    public QPSvo() {
    }

    public QPSvo(int qps) {
        this.qps = qps;
    }
}
