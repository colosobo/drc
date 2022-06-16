package com.timevale.drc.base.model.ext;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 增量子任务 拓展字段
 * */
@Data
public class DrcSubTaskIncrExt {

    /**
     * 是否支持ddl同步
     * */
    private Boolean supportDDLSync;

    private Boolean DDLSyncFilterDML;

    @JsonProperty(value = "DDLSyncFilterDML")
    public Boolean getDDLSyncFilterDML() {
        return DDLSyncFilterDML;
    }
}
