package com.timevale.drc.pd.service.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * @author gwk_2
 * @date 2021/3/2 11:38
 */
@Data
@ApiModel
public class DbConfigVO {

    private String url;
    private String username;
    private String pwd;
    private String database;


}
