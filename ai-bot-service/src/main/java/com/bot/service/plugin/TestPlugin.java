package com.bot.service.plugin;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.PrivateMessageHandler;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class TestPlugin extends BotPlugin{

    @Autowired
    private CompiledGraph sendMessageCompiledGraph;

    @PrivateMessageHandler
    @MessageHandlerFilter
    @Override
    public int onPrivateMessage(Bot bot , PrivateMessageEvent event){
//        log.info(event.getMessage());
//        sendMessageUtil.forwardMsg(2752662938L,event.getMessage());

        return MESSAGE_IGNORE;
    }
}
