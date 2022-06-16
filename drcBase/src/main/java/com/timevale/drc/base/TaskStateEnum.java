package com.timevale.drc.base;

import lombok.Getter;

/**
 * @author gwk_2
 * @date 2021/4/13 17:00
 * @description
 */
@Getter
public enum TaskStateEnum {
    /**  */
    UN_KNOW(-1, "不知道"),
    INIT(0, "初始化"),
    RUNNING(1, "运行中"),
    OVER(2, "正常结束"),
    HAND_STOP(3, "手动停止"),
    EXCEPTION(4, "任务异常"),
    SPLIT_ING(6, "拆分中"),
    SPLIT_OVER(7, "拆分结束"),
    STAGING(8, "暂存中.."),
    PLAYBACK_ING(9, "增量回放中"),
    DB_RUNNING_RPC_EXCEPTION(10, "db是运行时,但 RPC 调用异常");


    public int code;
    public String desc;

    TaskStateEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TaskStateEnum conv(int code) {
        for (TaskStateEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }


}
