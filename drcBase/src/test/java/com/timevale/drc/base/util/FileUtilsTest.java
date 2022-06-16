package com.timevale.drc.base.util;

import org.junit.Test;

public class FileUtilsTest {

    @Test
    public void readFileFromOffset() {
        String res = FileUtils.readFileFromOffset("../bizLog/sand_hex_test_ADD_ALL_Incr.log", 3, "UTF-8");
        System.out.println(res);
    }
}
