package com.timevale.drc.base.util;

import com.timevale.drc.base.serialize.JsonStringSerializer;
import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author gwk_2
 * @date 2021/1/28 22:32
 */
@Slf4j
@Component
public class DrcZkClient extends ZkClient {

    public DrcZkClient(@Value("${drc.zk.addr}") String zkAddr) {
        super(zkAddr);
        setZkSerializer(new JsonStringSerializer());
    }


}
