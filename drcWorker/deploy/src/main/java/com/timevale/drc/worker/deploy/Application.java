package com.timevale.drc.worker.deploy;

import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;


@MapperScan("com.timevale.drc.base.dao")
@SpringBootApplication(scanBasePackages = {"com.timevale.drc.worker", "com.timevale.drc.base"})
@EnableApolloConfig
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
