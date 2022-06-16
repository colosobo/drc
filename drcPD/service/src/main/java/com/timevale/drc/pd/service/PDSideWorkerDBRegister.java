package com.timevale.drc.pd.service;

import com.ctrip.framework.apollo.ConfigService;
import com.timevale.drc.base.Task;
import com.timevale.drc.base.Worker;
import com.timevale.drc.base.dao.DrcMachineRegisterTableMapper;
import com.timevale.drc.base.distributed.EndpointParser;
import com.timevale.drc.base.distributed.TaskRegister;
import com.timevale.drc.base.distributed.WorkerDistributedRegister;
import com.timevale.drc.base.eventbus.DrcThreadPool;
import com.timevale.drc.base.model.DrcMachineRegisterTable;
import com.timevale.drc.base.redis.DrcLockFactory;
import com.timevale.drc.pd.service.worker.RpcWorker;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PD 中运行.
 */
@Slf4j
@Component
@Setter
public class PDSideWorkerDBRegister implements WorkerDistributedRegister {

    @Resource
    private DrcMachineRegisterTableMapper mapper;
    @Resource
    private DrcLockFactory lockFactory;
    @Resource
    private LoadBalancer<Worker, Task> loadBalancer;
    @Resource
    private TaskRegister taskRegister;

    private static final ScheduledExecutorService ste =
            DrcThreadPool.newScheduledThreadPool(1, "   WorkerDBRegister");

    @Override
    public Worker register(String key) {
        return null;
    }



    @PostConstruct
    @Override
    public void init() {
        // 扫描失效的 worker.
        ste.scheduleAtFixedRate(() -> {
            try {
                lockFactory.getLock("WorkerDBRegister").lockAndProtect(3, () -> {
                    final List<DrcMachineRegisterTable> list = mapper.selectAllWorkerModel();

                    int badCount = 0;

                    for (DrcMachineRegisterTable mode : list) {
                        final Integer timeout = ConfigService.getAppConfig().getIntProperty("worker.timeout.in.sec", 5);
                        // 查看时间是否超时.
                        if (mode.getUpdateTime().before(new Date(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(timeout)))) {
                            // 如果超时, 那么就删除.
                            unRegister(mode.getIpPort());
                            ++badCount;
                        }
                    }

                    if (badCount < 1) {
                        return;
                    }

                    log.info("有机器宕机,开始执行重负载均衡,宕机机器数 = " + badCount);

                    final List<DrcMachineRegisterTable> healthList = mapper.selectAll();
                    if (healthList.size() < 1) {
                        log.error("可能发生了网络故障, 所有的节点都没有了, 先不做任何操作.");
                        return;
                    }
                    final List<Worker> workerList = healthList.stream().map((Function<DrcMachineRegisterTable, Worker>) m
                            -> new RpcWorker(EndpointParser.fromIpPort(m.getIpPort()), m.getId())).collect(Collectors.toList());

                    loadBalancer.reBalance(workerList, taskRegister.list(), "WorkerDBRegister");
                });
            } catch (Exception e) {
                // ignore
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

    @Override
    public List<Worker> list() {
        final List<DrcMachineRegisterTable> list = mapper.selectAllWorker();
        return list.stream().map(s -> (Worker) new RpcWorker(EndpointParser.fromIpPort(s.getIpPort()), s.getId())).collect(Collectors.toList());

    }

    @Override
    public Worker get(String key) {
        final DrcMachineRegisterTable workerMode = mapper.selectByIpPort(key);
        final String ipPort = workerMode.getIpPort();
        return new RpcWorker(EndpointParser.fromIpPort(ipPort), workerMode.getId());
    }

}
