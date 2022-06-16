package com.timevale.drc.base.distributed;

/**
 * PD  注册中心
 */
public interface PDDistributedRegister extends DistributedRegister<String> {

    boolean renew(String key);
}
