
package com.timevale.drc.pd.service;

import com.ctrip.framework.apollo.ConfigService;
import com.timevale.drc.base.dao.DrcMachineRegisterTableMapper;
import com.timevale.drc.base.distributed.PDDistributedRegister;
import com.timevale.drc.base.eventbus.DrcThreadPool;
import com.timevale.drc.base.model.DrcMachineRegisterTable;
import com.timevale.drc.base.redis.DrcLockFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 基于 DB 实现的 PD 注册中心
 */
@Component
public class PDDBRegister implements PDDistributedRegister {

    @Resource
    private DrcMachineRegisterTableMapper mapper;
    @Resource
    private DrcLockFactory lockFactory;
    @Resource
    private PDServer pdServer;

    private final ScheduledExecutorService ste =
            DrcThreadPool.newScheduledThreadPool(1, "PDDBRegister");


    @Override
    public String register(String key) {
        DrcMachineRegisterTable mode = new DrcMachineRegisterTable();
        // 唯一索引.
        mode.setIpPort(key);
        mode.setType(DrcMachineRegisterTable.TYPE_PD);
        mapper.insertSelective(mode);
        return null;
    }

    @Override
    public boolean unRegister(String ipPortProcess) {
        int r = mapper.deleteByIpPortProcess(ipPortProcess);
        return r > 0;
    }

    @Override
    public List<String> list() {
        return mapper.selectAllPD();
    }

    @Override
    public String get(String key) {
        return key;
    }

    @PostConstruct
    @Override
    public void init() {
        String key = pdServer.getSelfEndpoint().getTcpUrl();

        register(key);

        // 续约.
        ste.scheduleAtFixedRate(() -> {
            renew(key);
        }, 1, 1, TimeUnit.SECONDS);

        // 扫描失效.
        ste.scheduleAtFixedRate(() -> lockFactory.getLock("PDDBRegister").lockAndProtect(3, () -> {
            final List<DrcMachineRegisterTable> list = mapper.selectAllPDModel();
            list.forEach(c -> {
                final Integer timeout = ConfigService.getAppConfig().getIntProperty("pd.timeout.in.sec", 5);
                // 查看时间是否超时.
                if (c.getUpdateTime().before(new Date(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(timeout)))) {
                    // 删除
                    unRegister(c.getIpPort());
                }
            });
        }), 1, 5, TimeUnit.SECONDS);
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
}
