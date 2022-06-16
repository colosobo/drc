package com.timevale.drc.pd.deploy.controller;

public class DbConfigValidator {

    public boolean valid(String url) {

        if (!url.endsWith(":3306")) {
            throw new RuntimeException("数据库 URL 必须包含 3306 端口");
        }
        return true;
    }
}
