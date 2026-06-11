package com.bot.service.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.bot.service.bean.ChatClientFactory;
import com.bot.service.enums.NodeEnum;
import com.bot.service.service.TbBotLifeDetailDoService;
import com.bot.service.service.TbBotPromptDoService;
import com.bot.service.utils.SendMessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Component
public class TransformTagMessageNode implements NodeAction {

    private final ChatClientFactory chatClientFactory;

    private final SendMessageUtil sendMessageUtil;

    private final TbBotLifeDetailDoService tbBotLifeDetailDoService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        Long userId = (Long)state.value("userId").orElse(0L);
        String generateMessage = (String)state.value("generateMessage").orElse("");
        String memoryString = state.value("memoryString", String.class).orElse("");

        ChatClient chatClient = chatClientFactory.createChatClient(userId, NodeEnum.GENERATE);

        String lifeDetail = tbBotLifeDetailDoService.getCurrentActivity(Long.valueOf(System.getProperty("BOT_ID")));

        PromptTemplate promptTemplate = null;
        try {
            promptTemplate = PromptTemplate.builder().template(
                    Files.readString(Paths.get("./md/MainPrompt1.md"))
            ).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Prompt prompt = promptTemplate.create(Map.of("generateMessage",generateMessage,
                "memoryString",memoryString,"lifeDetail",lifeDetail
                )
        );
        log.info("消息格式调整提示词：{}" , prompt);
        String formatOutPut = chatClient.prompt(prompt)
                .call().content();
        log.info("结果输出：{}" , formatOutPut);
        sendMessageUtil.sendChatMsg(userId,formatOutPut);
        return Map.of();
    }

}
