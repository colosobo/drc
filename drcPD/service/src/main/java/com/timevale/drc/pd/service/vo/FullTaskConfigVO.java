package com.timevale.drc.pd.service.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author gwk_2
 * @date 2021/3/2 11:38
 */
@Data
@ApiModel
public class FullTaskConfigVO {

    /** 配置的分片大小, 单位万 */
    @ApiModelProperty("每个分片的大致长度, 配置的分片大小")
    private int rangeSizeConfig;

    /** 表名 */
    @ApiModelProperty("表名, 换行符号分隔")
    private String tableName;

    /** 默认 * */
    @ApiModelProperty("默认 *, 如果不使用默认的, 需要使用逗号隔开")
    private String selectFieldList = "*";

    /** 拆分状态， 0未拆分， 1拆分中， 2已经拆分*/
    private Integer splitState;

    /** where 条件 */
    private String whereStatement;

    public FullTaskConfigVO(int rangeSizeConfig, String tableName) {
        this.rangeSizeConfig = rangeSizeConfig;
        this.tableName = tableName;
    }

    public FullTaskConfigVO() {
    }
}
