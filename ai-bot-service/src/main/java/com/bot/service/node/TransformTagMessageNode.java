package com.bot.service.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.bot.service.bean.ChatClientFactory;
import com.bot.service.enums.NodeEnum;
import com.bot.service.service.TbBotPromptDoService;
import com.bot.service.utils.SendMessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Component
public class TransformTagMessageNode implements NodeAction {

    private final ChatClientFactory chatClientFactory;

    private final SendMessageUtil sendMessageUtil;

    private final TbBotPromptDoService tbBotPromptDoService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        Long userId = (Long)state.value("userId").orElse(0L);
        String generateMessage = (String)state.value("generateMessage").orElse("");
        String memoryString = state.value("memoryString", String.class).orElse("");

        ChatClient chatClient = chatClientFactory.createChatClient(userId, NodeEnum.GENERATE);

        PromptTemplate promptTemplate = PromptTemplate.builder().template("""
                你是消息审核与分析转换专家
                你的任务是根据历史消息对话对准备回复的消息进行检查转换，转换可能对应的格式
                
                历史消息：{memoryString}
                
                准备回复的消息：{generateMessage}
                
                要求：
                1. 文字用标签包裹，  文本：<text>文字</text>  语音：<voice tone="撒娇">语音</voice>
                2. 标签之间用换行分割，最多三条
                3. 语音和文本不要来回穿插太规律
                4. 检查以上要求，如果格式符合要求不做修改。 不符合要求请转换成要求格式，超过三条只保留重点的三条
                5. 只输出标签不要输出其他的
                """).build();
        Prompt prompt = promptTemplate.create(Map.of("generateMessage",generateMessage,"memoryString",memoryString));
        log.info("消息格式调整提示词：{}" , prompt);
        String formatOutPut = chatClient.prompt(prompt)
                .call().content();
        log.info("结果输出：{}" , formatOutPut);
        sendMessageUtil.sendChatMsg(userId,formatOutPut);
        return Map.of();
    }

}
