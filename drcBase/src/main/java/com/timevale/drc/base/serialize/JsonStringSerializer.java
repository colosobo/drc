package com.timevale.drc.base.serialize;

import com.google.gson.Gson;
import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.ZkSerializer;

/**
 * @author gwk_2
 * @date 2021/1/28 22:27
 */
public class JsonStringSerializer implements ZkSerializer {

    Gson gson = new Gson();

    public JsonStringSerializer() {
    }

    @Override
    public Object deserialize(byte[] bytes) throws ZkMarshallingError {
        return new String(bytes);
    }

    @Override
    public byte[] serialize(Object data) throws ZkMarshallingError {
        if (data instanceof String) {
            return ((String) data).getBytes();
        }
        return gson.toJson(data).getBytes();
    }
}
