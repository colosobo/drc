package com.timevale.drc.base;

import com.timevale.drc.base.rpc.RpcResult;
import com.timevale.drc.base.serialize.GenericJackson2JsonSerializer;
import org.junit.Assert;
import org.junit.Test;

public class TaskMetricsTest {

    @Test
    public void currentQPS() {
        TaskMetrics taskMetrics = TaskMetrics.Factory.create(2);
        GenericJackson2JsonSerializer genericJackson2JsonSerializer = new GenericJackson2JsonSerializer();
        String serializer = genericJackson2JsonSerializer.serializer(RpcResult.create(taskMetrics));
        RpcResult<TaskMetrics> o = genericJackson2JsonSerializer.deSerializer(serializer);
        System.out.println(o);
        Assert.assertNotNull(o);
    }


}
