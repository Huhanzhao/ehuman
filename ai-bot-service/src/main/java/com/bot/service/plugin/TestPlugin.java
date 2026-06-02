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
////        log.info(event.getMessage());
////        sendMessageUtil.forwardMsg(2752662938L,event.getMessage());
//        String target = """
//            ## 目标：请基于你当前在做的事情和状态还有历史消息，生成一条主动发给用户的自然消息,一行或者两行。
//            不要太突兀，要与上下文相和谐
//            1. 可以分享正在做的事、表达想念、撒娇、问好等。
//            """;
//        sendMessageCompiledGraph.invoke(Map.of("userId", 2752662938L, "target", target,"initiative",true));
        return MESSAGE_IGNORE;
    }
}
