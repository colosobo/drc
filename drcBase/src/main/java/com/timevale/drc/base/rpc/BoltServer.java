package com.timevale.drc.base.rpc;

import com.alipay.remoting.ConnectionEventProcessor;
import com.alipay.remoting.ConnectionEventType;
import com.alipay.remoting.rpc.RpcServer;
import com.alipay.remoting.rpc.protocol.UserProcessor;

/**
 * @author gwk_2
 * @date 2022/4/14 10:34
 */
public class BoltServer {

    /** port */
    private int       port;

    /** rpc server */
    private RpcServer server;

    public BoltServer(int port, boolean manageFeatureEnabled) {
        this.port = port;
        this.server = new RpcServer(this.port, manageFeatureEnabled);
    }

    public BoltServer(int port, boolean manageFeatureEnabled, boolean syncStop) {
        this.port = port;
        this.server = new RpcServer(this.port, manageFeatureEnabled, syncStop);
    }

    public boolean start() {
        return this.server.start();
    }

    public void stop() {
        this.server.stop();
    }

    public RpcServer getRpcServer() {
        return this.server;
    }

    public void registerUserProcessor(UserProcessor<?> processor) {
        this.server.registerUserProcessor(processor);
    }

    public void addConnectionEventProcessor(ConnectionEventType type,
                                            ConnectionEventProcessor processor) {
        this.server.addConnectionEventProcessor(type, processor);
    }
}
