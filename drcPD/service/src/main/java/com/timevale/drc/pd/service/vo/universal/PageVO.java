package com.timevale.drc.pd.service.vo.universal;

import lombok.Data;

import java.util.List;

/**
 * @author gwk_2
 * @date 2021/3/23 11:31
 */
@Data
public class PageVO<T> {

    Integer totalItems;

    Integer totalPages;

    Integer currentPage;

    Integer itemsPerPage;

    List<T> list;
}
