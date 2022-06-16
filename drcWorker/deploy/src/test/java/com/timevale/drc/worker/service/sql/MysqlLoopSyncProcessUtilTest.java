package com.timevale.drc.worker.service.sql;

import com.timevale.drc.worker.service.sink.mysql.MysqlLoopSyncProcessUtil;
import org.junit.Assert;
import org.junit.Test;

public class MysqlLoopSyncProcessUtilTest {

    @Test
    public void simpleTest(){
        Assert.assertFalse(MysqlLoopSyncProcessUtil.isFromDRCSql(""));
        Assert.assertFalse(MysqlLoopSyncProcessUtil.isFromDRCSql(" "));
        Assert.assertFalse(MysqlLoopSyncProcessUtil.isFromDRCSql(null));
        Assert.assertFalse(MysqlLoopSyncProcessUtil.isFromDRCSql("select * from aaa"));
        Assert.assertTrue(MysqlLoopSyncProcessUtil.isFromDRCSql(MysqlLoopSyncProcessUtil.addDRCUniqueFlag("select * from aaa")));
    }
}
