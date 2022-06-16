package com.timevale.drc.worker.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.timevale.drc.pd.facade.api.PdService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.*;

/**
 * @author gwk_2
 * @date 2021/12/20 23:28
 */
@Component
@Slf4j
public class RestartController {

    public static LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private static final ExecutorService executorService =
            new ThreadPoolExecutor(1,1, 60, TimeUnit.SECONDS,
                    new SynchronousQueue<>(), new ThreadFactoryBuilder().setNameFormat("RestartController").build());

    @Resource
    private PdService pdService;

    private volatile boolean running;

    @PostConstruct
    public void init() {
        running = true;
        executorService.submit(() -> {
            while (running) {
                try {
                    String take = queue.take();
                    if (!StringUtils.isBlank(take)) {
                        pdService.restart(take);
                    }
                } catch (InterruptedException e1) {
                    //ignore
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                }
            }

            log.info("RestartController executorService exit. queue size = {}", queue.size());
        });
    }

    public static void putRestartTask(String taskName) {
        try {
            queue.put(taskName);
        } catch (InterruptedException e) {
            log.error(e.getLocalizedMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        executorService.shutdownNow();
    }
}
