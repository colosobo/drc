package com.timevale.drc.base;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author gwk_2
 * @date 2021/1/29 00:59
 */
@ToString
@Data
public class DefaultEndpoint implements Endpoint , Serializable {

    private String ip;
    private int port;

    public static DefaultEndpoint buildFromIpPort(String ipPort) {
        String[] split = ipPort.split(":");
        String ip = split[0];
        int port = Integer.parseInt(split[1]);
        return new DefaultEndpoint(ip, port);
    }

    public DefaultEndpoint() {
    }

    public DefaultEndpoint(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    public String getIp() {
        return ip;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getHttpUrl() {
        return String.format("http://%s:%d", ip, port);
    }

    @Override
    public String getTcpUrl() {
        return String.format("%s:%d", ip, port);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultEndpoint that = (DefaultEndpoint) o;
        return port == that.port &&
                Objects.equals(ip, that.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }
}
