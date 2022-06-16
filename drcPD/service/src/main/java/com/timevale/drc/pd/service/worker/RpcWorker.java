package com.timevale.drc.pd.service.worker;

import com.timevale.drc.base.Endpoint;
import com.timevale.drc.base.Worker;
import com.timevale.drc.base.serialize.GenericJackson2JsonSerializer;

import java.util.Objects;

public class RpcWorker implements Worker {

    private final static GenericJackson2JsonSerializer serializer = new GenericJackson2JsonSerializer();

    private final Endpoint endpoint;
    private final int id;

    public RpcWorker(Endpoint endpoint, int id) {
        this.endpoint = endpoint;
        this.id = id;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public Endpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RpcWorker that = (RpcWorker) o;

        return Objects.equals(endpoint, that.endpoint);
    }

    @Override
    public int hashCode() {
        return endpoint != null ? endpoint.hashCode() : 0;
    }
}
