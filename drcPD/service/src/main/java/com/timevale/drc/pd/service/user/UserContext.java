package com.timevale.drc.pd.service.user;


import lombok.extern.slf4j.Slf4j;

/**
 * 基于线程的用户上下文.
 *
 * @author 莫那·鲁道
 * @date 2019-09-18-14:30
 */
@Slf4j
public class UserContext {

    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    public static UserInfo get() {
        UserInfo userInfo = THREAD_LOCAL.get();
        if (userInfo == null) {
            return new UserInfo();
        }
        return userInfo;
    }

    public static String getUserId() {
        return get().getId();
    }

    public static String getAlias() {
        return get().getAlias();
    }

    public static void set(UserInfo info) {
        THREAD_LOCAL.set(info);
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }


}
