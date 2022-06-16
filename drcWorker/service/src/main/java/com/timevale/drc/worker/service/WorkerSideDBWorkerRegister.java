package com.timevale.drc.worker.service;

import com.timevale.drc.base.Endpoint;
import com.timevale.drc.base.Worker;
import com.timevale.drc.base.dao.DrcMachineRegisterTableMapper;
import com.timevale.drc.base.distributed.WorkerDistributedRegister;
import com.timevale.drc.base.eventbus.DrcThreadPool;
import com.timevale.drc.base.model.DrcMachineRegisterTable;
import lombok.Setter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * worker 中运行.
 */
@Component
@Setter
public class WorkerSideDBWorkerRegister implements WorkerDistributedRegister {

    @Resource
    private DrcMachineRegisterTableMapper mapper;
    @Resource
    private WorkerServer workerServer;

    private final ScheduledExecutorService ste =
            DrcThreadPool.newScheduledThreadPool(1, "WorkerDBRegister");

    @Override
    public Worker register(String key) {

        DrcMachineRegisterTable mode = new DrcMachineRegisterTable();
        mode.setIpPort(key);
        mode.setType(DrcMachineRegisterTable.TYPE_WORKER);

        mapper.insertSelective(mode);

        // ignore return
        return null;
    }


    @PostConstruct
    @Override
    public void init() {
        final Endpoint selfEndpoint = workerServer.getSelfEndpoint();
        String key = selfEndpoint.getTcpUrl();

        register(key);

        ste.scheduleAtFixedRate(() -> {
            boolean renew = renew(key);
            if (!renew) {
                // 如果发生了网络抖动, 那么就可能被删掉, 那么就重新注册.
                register(key);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }


    @PreDestroy
    @Override
    public void stop() {
        ste.shutdown();
    }

    @Override
    public boolean renew(String key) {
        return mapper.renew(key) > 0;
    }

    @Override
    public boolean unRegister(String key) {
        int r = mapper.deleteByIpPortProcess(key);
        return r > 0;
    }

}
