package com.timevale.drc.base.rpc;

import lombok.Data;

/**
 * @author gwk_2
 * @date 2021/3/23 22:16
 */
@Data
public class RpcResult<T> {

    private T t;

    public static <T> RpcResult<T> create(T t) {
        RpcResult<T> rpcResult = new RpcResult<>();
        rpcResult.setT(t);
        return rpcResult;
    }
}
