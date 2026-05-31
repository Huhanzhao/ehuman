package com.bot.service.plugin;

import com.bot.service.bean.ChatClientFactory;
import com.bot.service.utils.SendMessageUtil;
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
    private ChatClientFactory chatClientFactory;

    @Autowired
    private SendMessageUtil sendMessageUtil;
    @PrivateMessageHandler
    @MessageHandlerFilter
    @Override
    public int onPrivateMessage(Bot bot , PrivateMessageEvent event){

//        sendMessageUtil.sendVoiceMsg(2752662938L,event.getMessage());
        return MESSAGE_IGNORE;
    }
}
