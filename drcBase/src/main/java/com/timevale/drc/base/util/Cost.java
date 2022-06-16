package com.timevale.drc.base.util;

/**
 * @author gwk_2
 * @date 2021/2/3 11:18
 */
public class Cost {

    private final long start;

    public Cost(long start) {
        this.start = start;
    }

    public static Cost start() {
        return new Cost(System.currentTimeMillis());
    }

    public long end() {
        return System.currentTimeMillis() - start;
    }
}
