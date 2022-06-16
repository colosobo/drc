package com.timevale.drc.base.distributed;

import com.beust.jcommander.internal.Lists;
import com.timevale.drc.base.Endpoint;
import com.timevale.drc.base.Worker;

import java.util.List;

/**
 * worker 注册中心
 */
public interface WorkerDistributedRegister extends DistributedRegister<Worker> {

    Worker mush = new Worker() {
        @Override
        public int id() {
            return 0;
        }

        @Override
        public Endpoint getEndpoint() {
            return null;
        }

    };

    /**
     * 续约.
     *
     * @param key 唯一 key, ip:port
     * @return
     */
    boolean renew(String key);

    @Override
    default List<Worker> list() {
        return Lists.newArrayList();
    }

    @Override
    default Worker get(String key) {
        return mush;
    }
}
