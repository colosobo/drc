package com.timevale.drc.base.sinkConfig;

import com.timevale.drc.base.TaskTypeEnum;
import lombok.Getter;

/**
 * @author gwk_2
 * @date 2021/4/22 14:04
 * @description
 */
public interface SinkConfig {

    /**
     * 获取消息格式, 0:drc, 2:canal
     * @return
     */
    int getMessageFormatType();

    /**
     * 设置任务类型.
     * @param taskTypeEnum 类型
     * @return
     */
    default SinkConfig taskType(TaskTypeEnum taskTypeEnum){ return this;};

    @Getter
    enum Type {
        /** ignore */
        UN_KNOW(0),
        KAFKA(1),
        MYSQL(2),
        ROCKETMQ(3),
        CANAL_KAFKA(4);

        int code;

        Type(int code) {
            this.code = code;
        }
    }
}
