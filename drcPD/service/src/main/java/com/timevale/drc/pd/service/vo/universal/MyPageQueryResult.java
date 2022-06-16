package com.timevale.drc.pd.service.vo.universal;

import com.timevale.drc.base.web.BaseResult;
import lombok.Data;

import java.util.List;

/**
 * @author gwk_2
 * @date 2021/3/22 17:17
 */
@Data
public class MyPageQueryResult<T> extends BaseResult {

    private DrcData<PageVO<T>> data = new DrcData<>();

    public MyPageQueryResult() {
        super(true, "SUCCESS");
        this.data.resultObject = new PageVO<>();
    }

    public void setTotalItems(Integer totalItems) {
        this.data.resultObject.totalItems = totalItems;
    }

    public void setTotalPages(Integer totalPages) {
        this.data.resultObject.totalPages = totalPages;
    }

    public void setCurrentPage(Integer currentPage) {
        this.data.resultObject.currentPage = currentPage;
    }

    public void setItemsPerPage(Integer itemsPerPage) {
        this.data.resultObject.itemsPerPage = itemsPerPage;
    }

    public void setResultList(List<T> resultList) {
        this.data.resultObject.list = resultList;
    }
}
