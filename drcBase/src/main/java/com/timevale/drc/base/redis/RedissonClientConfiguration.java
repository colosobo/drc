package com.timevale.drc.base.redis;

import com.timevale.drc.base.rpc.HostNameUtil;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author gwk_2
 * @date 2022/1/10 17:14
 */
@Configuration
public class RedissonClientConfiguration {


    @Value("${redis.host}")
    private String host;

    @Value("${redis.password}")
    private String pwd;

    @Bean
    public RedissonClient redisson() {
        Config config = new Config();

        config.useSingleServer()
                .setDnsMonitoringInterval(-1)
                .setTimeout(3000)
                .setPassword(pwd)
                .setAddress("redis://" + host + ":6379");

        RedissonClient redisson = Redisson.create(config);
        DrcRedisson.setRedisson(redisson);
        return redisson;
    }
}
