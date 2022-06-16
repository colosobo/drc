package com.timevale.drc.worker.deploy.api;

import com.timevale.drc.base.Task;
import com.timevale.drc.base.TaskStateEnum;
import com.timevale.drc.base.rpc.RpcResult;
import com.timevale.drc.base.serialize.GenericJackson2JsonSerializer;
import com.timevale.drc.worker.service.WorkerServer;
import com.timevale.drc.worker.service.exp.NotFoundTaskException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.UndeclaredThrowableException;

/**
 * 针对 PD 发起的 task 调用
 *
 * @author gwk_2
 * @date 2021/1/28 23:40
 * @see Task
 */
@RestController
@Slf4j
@RequestMapping("/api/task")
public class DrcWorkerTaskApiController {

    @Value("${log.line.default:40}")
    private Integer logLineDefault;

    private final GenericJackson2JsonSerializer serializer = new GenericJackson2JsonSerializer();
    public static final String SUCCESS = "success";

    @Autowired
    private WorkerServer workerServer;

    /**
     * @param taskName
     * @return
     * @see Task#start()
     */
    @PostMapping(value = "/{taskName}/start")
    public String start(@PathVariable("taskName") String taskName) {
        try {
            workerServer.start(taskName);
        } catch (NotFoundTaskException notFoundTaskException) {
            // ignore
            log.warn(notFoundTaskException.getMessage());
        } catch (UndeclaredThrowableException e) {
            log.error(e.getMessage(), e);
            return serializer.serializer(RpcResult.create(e.getUndeclaredThrowable().getCause()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return serializer.serializer(RpcResult.create(e));
        }
        return serializer.serializer(RpcResult.create(SUCCESS));
    }

    /**
     * @param taskName
     * @return
     * @see Task#stop(java.lang.String)
     */
    @PostMapping("/{taskName}/stop/{cause}")
    public String stop(@PathVariable("taskName") String taskName,
                       @PathVariable("cause") String cause) {
        try {
            workerServer.stop(taskName, cause);
        } catch (UndeclaredThrowableException e) {
            log.error(e.getMessage(), e);
            return serializer.serializer(RpcResult.create(e.getUndeclaredThrowable().getCause()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return serializer.serializer(RpcResult.create(e));
        }
        return serializer.serializer(RpcResult.create(SUCCESS));
    }

    /**
     * @param taskName
     * @return
     * @see Task#getState()
     */
    @PostMapping(value = "/{taskName}/getState")
    public String getState(@PathVariable("taskName") String taskName) {
        TaskStateEnum state = workerServer.getState(taskName);
        return serializer.serializer(RpcResult.create(state));
    }

    /**
     * @param taskName
     * @return
     * @see Task#isRunning()
     */
    @PostMapping("/{taskName}/isRunning")
    public String isRunning(@PathVariable("taskName") String taskName) {
        boolean result = workerServer.isRunning(taskName);
        return serializer.serializer(RpcResult.create(result));
    }

    /**
     * @param taskName
     * @return
     * @see Task#getType()
     */
//    @PostMapping("/{taskName}/getType")
//    public String getType(@PathVariable("taskName") String taskName) {
//        return serializer.serializer(RpcResult.create(workerServer.getType(taskName)));
//    }

    /**
     * @param taskName
     * @return
     * @see Task#getName()
     */
//    @PostMapping("/{taskName}/getName")
//    public String getName(@PathVariable("taskName") String taskName) {
//        return serializer.serializer(RpcResult.create(workerServer.getName(taskName)));
//    }


    /**
     * @param taskName
     * @return
     * @see Task#getLogText(int)
     */
    @PostMapping("/{taskName}/getLogText")
    public String getLogText(@PathVariable("taskName") String taskName) {
        return serializer.serializer(RpcResult.create(workerServer.getLogText(taskName, logLineDefault)));
    }

}
