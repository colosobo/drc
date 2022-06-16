package com.timevale.drc.pd.service.vo;

import com.timevale.drc.base.model.ext.DrcSubTaskIncrExt;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author gwk_2
 * @date 2021/3/9 20:02
 */
@Data
@ApiModel
public class TaskVO {

    @ApiModel
    @Data
    public static class FullTaskVO extends BaseTaskVO {
        @ApiModelProperty("分片主键名称")
        private String slicePkName;
        @ApiModelProperty("分片最小主键")
        private String sliceMinPk;
        @ApiModelProperty("分片最大主键")
        private String sliceMaxPk;
        @ApiModelProperty("分片实际大小")
        private Integer rangeSize;
        @ApiModelProperty("当前扫描到的主键")
        private String cursor;
        @ApiModelProperty("拆分状态， 0未拆分， 1拆分中， 2已经拆分结束")
        private Integer splitState;
        @ApiModelProperty("表名")
        private String tableName;
        @ApiModelProperty("拆分的分片总数")
        private Integer sliceCount;
        @ApiModelProperty("已经 select 完的分片数字")
        private Integer finishSliceCount;
        @ApiModelProperty("完成的行数.")
        private Long finishRowCount;

    }

    @ApiModel
    @Data
    public static class IncrTaskVO extends BaseTaskVO {
        @ApiModelProperty("增量表过滤表达式")
        private String tableExpression;

        @ApiModelProperty("拓展字段配置")
        private DrcSubTaskIncrExt ext;
    }

}

