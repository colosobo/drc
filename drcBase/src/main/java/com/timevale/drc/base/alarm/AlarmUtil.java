package com.timevale.drc.base.alarm;

import com.ctrip.framework.apollo.ConfigService;
import com.google.common.collect.Lists;
import com.timevale.drc.base.eventbus.DrcThreadPool;
import com.timevale.drc.base.eventbus.DrcThreadPoolExecutor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;

/**
 * @author gwk_2
 * @date 2020/8/19 21:53
 */
@Setter
@Slf4j
@Service
public class AlarmUtil {
    private static DrcThreadPoolExecutor executor = DrcThreadPool.createThreadPool("AlarmUtil");

    /*private static AlarmClient alarmClient;

    @Value("${alarm.domain}")
    private String alarmDomain;

    @Value("${env}")
    private String env;*/

    /**
     * 推送消息给 admin
     */
    public static void pushAlarm2Admin(String content) {

        String adminString = ConfigService.getAppConfig().getProperty("alarm.admin", "xuanmie,shanreng");
        List<String> nickNames = Lists.newArrayList(adminString.split(","));

        DrcAlarmModel drcAlarmModel = new DrcAlarmModel(content, nickNames);

        executor.execute(() -> {
            try {
                //alarmClient.alarm(drcAlarmModel.getContent(), (drcAlarmModel.getNickNames()));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
    }

    @PostConstruct
    public void init() {
        // 提供静态调用.
        //alarmClient = new AlarmClient(alarmDomain, env);
    }

    @PreDestroy
    public void destroy() {
        executor.shutdown();
    }
}
