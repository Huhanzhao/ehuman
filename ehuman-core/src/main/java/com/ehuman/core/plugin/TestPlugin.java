package com.ehuman.core.plugin;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.ehuman.core.node.test.test1;
import com.ehuman.core.node.test.test2;
import com.ehuman.core.service.CommonAuxiliaryService;
import com.ehuman.core.service.EmbeddingService;
import com.ehuman.core.utils.SendMessageUtil;
import com.ehuman.dao.service.*;
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
    private TbMessageSummaryVectorDoService tbMessageSummaryVectorDoService;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private DBVectorOperationService dbVectorOperationService;


    @Autowired
    private TbUserBotStatusDoService tbUserBotStatusDoService;
    @Autowired
    private TbAiChatMemoryService tbAiChatMemoryService;
    @Autowired
    private TbBotLifeDetailDoService tbBotLifeDetailDoService;

    @PrivateMessageHandler
    @MessageHandlerFilter
    @Override
    public int onPrivateMessage(Bot bot, PrivateMessageEvent event)  {
//

//        // 调用向量模型 向量化问题
//        float[] floats = embeddingService.embeddingText(event.getUserId(), event.getMessage());
//        // 检索记忆
//        List<TbMessageVectorSearchDo> tbMessageVectorSearchDos = dbVectorOperationService.searchVector(event.getUserId(),
//                Arrays.toString(floats),
//                2,
//                0.6);

        return MESSAGE_IGNORE;

//        sendMessageUtil.sendVoiceMsg(event.getUserId(),"<voice tone=\"开心\">"
//                + event.getMessage() + "</voice>");
//        return MESSAGE_BLOCK;
    }

}
