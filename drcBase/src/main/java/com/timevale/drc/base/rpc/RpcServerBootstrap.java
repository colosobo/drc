package com.timevale.drc.base.rpc;

import com.alipay.remoting.rpc.protocol.UserProcessor;

/**
 * @author gwk_2
 * @date 2022/4/14 10:40
 */
public class RpcServerBootstrap {

    private BoltServer server;

    public void boot(int port, UserProcessor<?> processor) {
        server = new BoltServer(port, true);
        server.registerUserProcessor(processor);
        server.start();
    }

    public void shutdown() {
        server.stop();
    }

}
