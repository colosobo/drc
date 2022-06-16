package com.timevale.drc.base.rpc;

import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.protocol.SyncUserProcessor;
import com.timevale.drc.base.DefaultEndpoint;
import com.timevale.drc.base.eventbus.DrcThreadPool;
import com.timevale.drc.base.eventbus.DrcThreadPoolExecutor;
import org.junit.Test;

public class RpcServerBootstrapTest {

    @Test
    public void boot() throws InterruptedException {

        RpcServerBootstrap rpcServerBootstrap = new RpcServerBootstrap();
        rpcServerBootstrap.boot(8099, new SyncUserProcessor<RpcRequest<Integer>>() {
            @Override
            public Object handleRequest(BizContext bizContext, RpcRequest<Integer> o) throws Exception {
                System.out.println("=====>>>" + o);
                return new DefaultEndpoint("192.168.1.1", 8080);
            }

            @Override
            public String interest() {
                return RpcRequest.class.getName();
            }
        });


        DrcThreadPoolExecutor threadPool = DrcThreadPool.createThreadPool("", 10, 10);

        BoltClient rpcClient = new BoltClient();
        rpcClient.start();

        for (int i = 0; i < 10; i++) {
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    DefaultEndpoint result = rpcClient.send("localhost:8099", new RpcRequest<>(123));

                    System.out.println("result = " + result);
                }
            });
        }

        Thread.sleep(12121);

    }

    @Test
    public void shutdown() {
    }
}
