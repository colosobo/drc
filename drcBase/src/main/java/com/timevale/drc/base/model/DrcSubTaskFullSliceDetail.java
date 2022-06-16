package com.timevale.drc.base.model;

import lombok.Data;
import lombok.ToString;

import javax.persistence.Table;

/**
 * @author gwk_2
 * @date 2021/3/2 11:28
 */
@ToString
@Data
@Table(name = "drc_sub_task_full_slice_detail")
public class DrcSubTaskFullSliceDetail extends BaseTaskModel {

    /** 不是最后一个分片 */
    public static final Integer IS_LAST_SLICE_FALSE = 0;
    /** 是最后一个分片 */
    public static final Integer IS_LAST_SLICE_TRUE = 1;

    /** 分片序号 */
    private Integer sliceNumber;

    /** 主键名称 */
    private String slicePkName;

    /** 分片区间最小 主键 */
    private String sliceMinPk;

    /** 分片区间最大 主键 */
    private String sliceMaxPk;

    /** 这个分片的实际 size */
    private Integer rangeSize;

    /** 是否是最后一个分片 */
    private Integer isLastSlice;

    /** 游标，即扫描到哪个 id 了 */
    private String sliceCursor;

    /** 冗余字段. */
    private Integer drcSubTaskFullConfigId;

}
