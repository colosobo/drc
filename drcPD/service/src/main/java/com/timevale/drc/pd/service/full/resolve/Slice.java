package com.timevale.drc.pd.service.full.resolve;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 分片模型
 *
 * @author gwk_2
 * @date 2021/3/5 20:25
 */
@ToString
@Getter
@Setter
@Data
public class Slice {

    public static final String EMPTY_STRING = "EMPTY";

    public static final Slice EMPTY = new Slice(EMPTY_STRING);

    int sliceNumber;
    String sliceName;
    String minPkValue;
    String maxPkValue;
    int rangeSize;
    Integer parentId;
    /**
     * 如果是最后一个 range, 那么, 就需要使用 <= maxRange 号.
     */
    boolean isLastRange;
    Integer drcSubTaskFullConfigId;
    /** 主键名称 */
    String slicePkName;

    public Slice() {
    }

    public Slice(String sliceName) {
        this.sliceName = sliceName;
    }
}
