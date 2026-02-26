package com.helper.services.plugin;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.helper.services.entity.TbAiChatMemoryDo;
import com.helper.services.entity.TbChatConfigDo;
import com.helper.services.entity.TbUserGraceDo;
import com.helper.services.entity.TbUserModelConfigDo;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.PrivateMessageHandler;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import com.mikuac.shiro.model.ArrayMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.*;


@Component
@Slf4j
public class TestPlugin extends BotPlugin{

    @PrivateMessageHandler
    @MessageHandlerFilter
    @Override
    public int onPrivateMessage(Bot bot , PrivateMessageEvent event){
        String userInput = event.getMessage();
        String build = MsgUtils.builder().face(123).build();
//        bot.sendPrivateMsg(event.getUserId(), build + "sdfasdfasd",false);
//
//            bot.sendPrivateMsg(event.getUserId(), event.getMessage(),false);

        return MESSAGE_IGNORE;
    }
}
