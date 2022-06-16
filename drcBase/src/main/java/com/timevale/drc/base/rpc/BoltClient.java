package com.timevale.drc.base.rpc;

import com.alipay.remoting.rpc.RpcClient;
import com.alipay.remoting.rpc.protocol.UserProcessor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gwk_2
 * @date 2022/4/14 10:42
 */
@Slf4j
@Data
public class BoltClient {

    private RpcClient client = new RpcClient();

    public void start(UserProcessor<?> processor) {
        client = new RpcClient();
        client.registerUserProcessor(processor);
        client.init();
    }

    public void start() {
        client = new RpcClient();
        client.init();
    }

    public <T> T send(String address, Object req) {
        try {
            return (T) client.invokeSync(address, req, 3000);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


}
