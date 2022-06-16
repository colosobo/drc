package com.timevale.drc.base;

/**
 * @author gwk_2
 * @date 2021/1/28 21:51
 * @description
 */
public interface Endpoint {

    String getIp();

    int getPort();

    String getHttpUrl();

    String getTcpUrl();

}
