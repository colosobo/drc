package com.timevale.drc.worker.service.task.mysql.full.event;

import com.timevale.drc.base.eventbus.Subscriber;
import com.timevale.drc.pd.facade.api.PdService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 反注册事件.
 *
 * @author gwk_2
 * @date 2021/3/23 23:45
 */
@Component
@Slf4j
public class UnRegisterSubscriber extends Subscriber<UnRegisterEvent> {

    @Autowired
    private PdService pdService;

    public UnRegisterSubscriber() {
        super(UnRegisterEvent.class);
    }

    @SneakyThrows
    @Override
    public void onEvent(UnRegisterEvent event) {
        String data = event.data();
        // 取消注册此任务.
        log.info("调用 PD 取消注册此任务 : {}", data);
        // 慢点
        Thread.sleep(100);
        pdService.unRegister(data);
    }
}
