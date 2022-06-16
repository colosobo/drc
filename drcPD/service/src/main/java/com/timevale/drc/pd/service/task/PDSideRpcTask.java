package com.timevale.drc.pd.service.task;

import com.timevale.drc.base.*;
import com.timevale.drc.base.redis.DrcRedisson;
import com.timevale.drc.base.rpc.RpcResult;
import com.timevale.drc.base.serialize.GenericJackson2JsonSerializer;
import com.timevale.drc.base.util.DrcHttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import java.util.concurrent.TimeUnit;

@Slf4j
public class PDSideRpcTask implements Task {

    private static final GenericJackson2JsonSerializer serializer = new GenericJackson2JsonSerializer();

    private final Router<Worker> router;

    public PDSideRpcTask(Router<Worker> router) {
        this.router = router;
    }

    @Override
    public String getName() {
        return router.getName();
    }

    @Override
    public TaskMetrics metrics() {
        return Task.super.metrics();
    }

    @Override
    public TaskStateEnum getState() {
        String path = buildUrl(router.getRoute().getEndpoint().getHttpUrl(), router.getName(), "getState", null);
        try {
            return (TaskStateEnum) invokeRpc(router, path, 0);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getLogText(int line) {
        String path = buildUrl(router.getRoute().getEndpoint().getHttpUrl(), router.getName(), "getLogText", null);
        try {
            return (String) invokeRpc(router, path, 0);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() {
        String path = buildUrl(router.getRoute().getEndpoint().getHttpUrl(), router.getName(), "start", null);
        try {
            invokeRpc(router, path, 0);
            DrcRedisson.set("DRC_TASK_RECENT_START_IP_" + router.getName(),
                    router.getRoute().getEndpoint().getIp(), 60, TimeUnit.DAYS);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop(String cause) {
        String path = buildUrl(router.getRoute().getEndpoint().getHttpUrl(), router.getName(), "stop", cause);
        try {
            invokeRpc(router, path, 0);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isRunning() {
        String path = buildUrl(router.getRoute().getEndpoint().getHttpUrl(), router.getName(), "isRunning", null);
        try {
            return (boolean) invokeRpc(router, path, 0);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    private static Object invokeRpc(Router<Worker> router, String path, int retry) throws Throwable {
        try {
            CloseableHttpResponse response = DrcHttpClientUtil.post(path);
            String s = EntityUtils.toString(response.getEntity());

            if (!(response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 400)) {
                String msg = String.format("Worker Task=%s RPC 调用失败, path: %s,  响应内容: %s ", router.getName(), path, s);
                throw new RuntimeException(msg);
            }

            Object o = serializer.deSerializer(s);
            if (o instanceof String) {
                throw new RuntimeException(o.toString());
            }
            RpcResult<?> rpcResult = (RpcResult<?>) o;
            Object t = rpcResult.getT();
            if (t instanceof Exception) {
                throw (Exception) t;
            }
            return t;
        } catch (java.lang.reflect.UndeclaredThrowableException u) {
            throw u.getCause();
        } catch (Exception e) {
            if (retry < 2 && !path.contains("metrics")) {
                // 重试3次.
                return invokeRpc(router, path, ++retry);
            } else {
                if (!path.contains("metrics")) {
                    log.error("rpc 调用失败, path = {}, 异常原因 = {}", path, e.getMessage());
                }
                throw e;
            }
        }
    }


    private static String buildUrl(String httpUrl, String taskName, String methodName, String arg) {
        if (StringUtils.isBlank(arg)) {
            return String.format("%s/api/task/%s/%s", httpUrl, taskName, methodName);
        } else {
            return String.format("%s/api/task/%s/%s/%s", httpUrl, taskName, methodName, arg.replaceAll(" ", "_"));
        }
    }
}
