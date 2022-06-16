package com.timevale.drc.base;

import org.junit.Test;

public class DefaultEndpointTest {

    @Test
    public void getHttpUrl() {
        DefaultEndpoint defaultEndpoint = new DefaultEndpoint("localhost", 8080);
        System.out.println(defaultEndpoint.getHttpUrl());
    }
}
