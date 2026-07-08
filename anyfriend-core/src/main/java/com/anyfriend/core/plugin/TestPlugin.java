package com.anyfriend.core.plugin;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.anyfriend.core.node.test.test1;
import com.anyfriend.core.node.test.test2;
import com.anyfriend.core.service.CommonAuxiliaryService;
import com.anyfriend.core.utils.SendMessageUtil;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.PrivateMessageHandler;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class TestPlugin extends BotPlugin {

    @Autowired
    private CompiledGraph sendMessageCompiledGraph;

    @Autowired
    private SendMessageUtil sendMessageUtil;

    @Autowired
    private BotContainer container;
    @Autowired
    private test1 test1;
    @Autowired
    private test2 test2;

    @Autowired
    private CommonAuxiliaryService commonAuxiliaryService;

    @Autowired
    private TbFullContextSearchSummaryService tbFullContextSearchSummaryService;


    @PrivateMessageHandler
    @MessageHandlerFilter
    @Override
    public int onPrivateMessage(Bot bot, PrivateMessageEvent event)  {


        return MESSAGE_IGNORE;
    }

}
