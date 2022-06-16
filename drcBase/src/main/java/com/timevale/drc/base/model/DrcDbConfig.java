package com.timevale.drc.base.model;

import lombok.Data;

import javax.persistence.Table;

/**
 * @author gwk_2
 * @date 2021/3/2 10:50
 */
@Data
@Table(name = "drc_db_config")
public class DrcDbConfig extends BaseDO {

    private String url;

    private String username;

    private String password;

    private String databaseName;

    public DrcDbConfig(String url, String username, String password, String databaseName) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.databaseName = databaseName;
    }

    public DrcDbConfig() {
    }
}
