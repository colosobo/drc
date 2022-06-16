package com.timevale.drc.base.distributed;

import com.timevale.drc.base.DefaultEndpoint;
import com.timevale.drc.base.Endpoint;

public class EndpointParser {

    public static Endpoint fromIpPort(String ipPort) {
        final String[] split = ipPort.split(":");
        return new DefaultEndpoint(split[0], Integer.parseInt(split[1]));
    }
}
