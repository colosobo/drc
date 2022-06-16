package com.timevale.drc.base.rpc;

import lombok.Data;

import java.io.Serializable;

/**
 * @author gwk_2
 * @date 2022/5/1 11:25
 */
@Data
public class RpcRequest<T> implements Serializable {

    private T data;

    public RpcRequest() {
    }

    public RpcRequest(T data) {
        this.data = data;
    }
}
