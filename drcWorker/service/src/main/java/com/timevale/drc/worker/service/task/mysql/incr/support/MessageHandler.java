package com.timevale.drc.worker.service.task.mysql.incr.support;

import com.alibaba.otter.canal.protocol.Message;
import com.timevale.drc.base.binlog.Binlog2JsonModel;
import lombok.Getter;

import java.util.List;

/**
 * @author gwk_2
 * @date 2021/4/26 15:06
 */
public interface MessageHandler<M> {

    /**
     * 处理 Canal Server 的消息, 并解析成想要的消息格式返回.
     * @param message 原生消息.
     * @return 具体格式
     */
    List<M> handler(Message message);

    M convFromJson(String json);

    @Getter
    enum MessageType {
        /** DRC 格式文件 */
        DRC(1, Binlog2JsonModel.class),
        /** Canal 原生格式  */
        CANAL(2, Message.class);

        int code;
        Class<?> aClass;

        MessageType(int code, Class<?> aClass) {
            this.code = code;
            this.aClass = aClass;
        }

        public static MessageType conv(int type) {
            for (MessageType value : values()) {
                if (value.code == type) {
                    return value;
                }
            }
            return DRC;
        }
    }
}
