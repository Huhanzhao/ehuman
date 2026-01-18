package com.helper.services.plugin;

import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.PrivateMessageHandler;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;

@Component
@Shiro
public class BangumiPlugin extends BotPlugin {

    @PrivateMessageHandler
    @MessageHandlerFilter()
    public void privateHandler(Bot bot , PrivateMessageEvent event, Matcher matcher){

        String message = event.getMessage();
//        String aiMessage = aiChatService.simbleChat(event.getUserId().toString(), message, event.getUserId().toString());
        String aiMessage = "hello";
        String msg = MsgUtils.builder().text(aiMessage).build();

        bot.sendPrivateMsg(event.getUserId(),msg,false);
    }

}
