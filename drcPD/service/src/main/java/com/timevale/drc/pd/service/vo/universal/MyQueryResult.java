package com.timevale.drc.pd.service.vo.universal;

import com.timevale.drc.base.web.BaseResult;
import lombok.Data;

/**
 * @author gwk_2
 * @date 2021/3/22 17:10
 */
@Data
public class MyQueryResult<T> extends BaseResult {

    private DrcData<T> data = new DrcData<>();

    public MyQueryResult() {
        this(true, "SUCCESS");
    }

    public MyQueryResult(boolean success, String message) {
        super(success, message);
    }

    public MyQueryResult<T> setResultObject(T t) {
        data.setResultObject(t);
        return this;
    }

}
