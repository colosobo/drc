package com.timevale.drc.worker.deploy.api;

import com.timevale.drc.base.rpc.RpcResult;
import com.timevale.drc.base.serialize.GenericJackson2JsonSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author gwk_2
 * @date 2021/3/18 14:01
 */
@Slf4j
@RestController
@RequestMapping("/api/worker")
public class WorkerApiController {

    private final GenericJackson2JsonSerializer serializer = new GenericJackson2JsonSerializer();

    @PostMapping("/health")
    public String health() {
        try {
            return serializer.serializer(RpcResult.create("UP"));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return serializer.serializer(RpcResult.create(e));
        }
    }

}
