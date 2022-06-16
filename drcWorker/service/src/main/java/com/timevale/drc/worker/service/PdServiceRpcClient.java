package com.timevale.drc.worker.service;

import com.google.gson.Gson;
import com.timevale.drc.base.dao.DrcMachineRegisterTableMapper;
import com.timevale.drc.base.metrics.TaskMetricsModel;
import com.timevale.drc.base.util.DrcHttpClientUtil;
import com.timevale.drc.pd.facade.api.PdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author gwk_2
 * @date 2022/1/18 01:15
 */
@Service
@Slf4j
public class PdServiceRpcClient implements PdService {

    public static final String SUCCESS = "success";

    private final Gson gson = new Gson();

    @Resource
    private DrcMachineRegisterTableMapper machineRegisterTableMapper;

    @Override
    public String unRegister(String taskName) {
        return invoke("/unRegister/taskName", taskName);
    }

    @Override
    public String restart(String taskName) {
        return invoke("/restart/taskName", taskName);
    }

    @Override
    public String uploadTaskMetrics(Map<String, TaskMetricsModel> data) {
        return broadcast("/uploadTaskMetrics", gson.toJson(data));
    }

    private String broadcast(String url, String body) {
        List<String> pdList = machineRegisterTableMapper.selectAllPD();
        for (String ipPort : pdList) {
            try {
                String result = DrcHttpClientUtil.postAndReturnString("http://" + ipPort + url, body);
                if (SUCCESS.equalsIgnoreCase(result)) {
                    log.debug("rpc 调用成功, url = {}, body = {}, 结果:{}", url, body, result);
                } else {
                    log.warn("rpc 调用失败, url = {}, body = {}, 结果:{}", url, body, result);
                }
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
        }
        return "success";
    }

    private String invoke(String url, String body) {
        final List<String> pdList = machineRegisterTableMapper.selectAllPD();
        Collections.shuffle(pdList);
        for (String ipPort : pdList) {
            try {
                String result = DrcHttpClientUtil.postAndReturnString("http://" + ipPort + url, body);
                if (SUCCESS.equalsIgnoreCase(result)) {
                    return result;
                } else {
                    log.warn("rpc 调用失败, url = {}, body = {}, 结果:{}", url, body, result);
                }
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
        }
        return null;
    }

}
