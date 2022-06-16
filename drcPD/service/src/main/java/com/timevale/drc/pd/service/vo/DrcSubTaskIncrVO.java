package com.timevale.drc.pd.service.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class DrcSubTaskIncrVO {
    @ApiModelProperty("增量同步的 表过滤表达式.")
    private String tableExpression;

    @ApiModelProperty("是否支持DDL")
    private Boolean supportDDLSync;

    @ApiModelProperty("DDL同步时，是否过滤DML")
    private Boolean DDLSyncFilterDML;

    @JsonProperty(value = "DDLSyncFilterDML")
    public Boolean getDDLSyncFilterDML() {
        return DDLSyncFilterDML;
    }

    public DrcSubTaskIncrVO(String tableExpression) {
        this.tableExpression = tableExpression;
    }

    public DrcSubTaskIncrVO() {
    }

    public void setTableExpression(String tableExpression) {
        this.tableExpression = tableExpression;
    }
}
