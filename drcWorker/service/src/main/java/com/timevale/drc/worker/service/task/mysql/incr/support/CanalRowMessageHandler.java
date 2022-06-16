package com.timevale.drc.worker.service.task.mysql.incr.support;

import com.alibaba.otter.canal.protocol.Message;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author gwk_2
 * @date 2021/4/26 14:31
 */
@Slf4j
public class CanalRowMessageHandler implements MessageHandler<Message> {

    private Gson gson = new Gson();
    @Override
    public List<Message> handler(Message message) {
        return Lists.newArrayList(message);
    }

    @Override
    public Message convFromJson(String json) {
        return gson.fromJson(json, Message.class);
    }
}
