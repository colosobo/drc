package com.timevale.drc.worker.service.sink.util;

import com.timevale.drc.base.util.GlobalConfigUtil;
import com.timevale.drc.base.util.MysqlIncrTaskConstants;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * @author shanreng
 */
public class MysqlSinkUtil {

    public static int getDDLSyncTimeout(String taskName) {
        if (StringUtils.isBlank(taskName)) {
            return MysqlIncrTaskConstants.DEFAULT_DDL_TIMEOUT;
        }
        Map<String, Integer> configMap = GlobalConfigUtil.getMysqlDDLSyncTimeoutMap();
        return configMap.getOrDefault(taskName, MysqlIncrTaskConstants.DEFAULT_DDL_TIMEOUT);
    }
}
