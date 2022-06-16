package com.timevale.drc.base.rocketmq.admin;

import com.ctrip.framework.apollo.ConfigService;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.remoting.RPCHook;

public class AclUtil {


    public static RPCHook getAclRPCHook() {
        return getAclRPCHook(ConfigService.getAppConfig().getProperty("rocketmq.ak", null), ConfigService.getAppConfig().getProperty("rocketmq.sk", null));
    }


    public static RPCHook getAclRPCHook(String ak, String sk) {
        return new AclClientRPCHook(new SessionCredentials(ak, sk));
    }

}
