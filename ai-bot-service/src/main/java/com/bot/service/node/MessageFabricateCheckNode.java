package com.bot.service.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.bot.service.bean.ChatClientFactory;
import com.bot.service.enums.NodeEnum;
import com.bot.service.utils.SendMessageUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class MessageFabricateCheckNode implements NodeAction {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(MessageFabricateCheckNode.class);
    private final ChatClientFactory chatClientFactory;

    private final SendMessageUtil sendMessageUtil;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        // todo 如果加入了历史消息和总结消息，也要在此节点加入
        Long userId = (Long) state.value("userId").orElse(0L);
        String generateMessage = (String) state.value("generateMessage").orElse("");
        String memoryString = state.value("memoryString", String.class).orElse("");

        ChatClient chatClient = chatClientFactory.createChatClient(userId, NodeEnum.GENERATE);

        PromptTemplate promptTemplate = PromptTemplate.builder().template("""
                你是消息审核专家
                你的任务是根据历史消息对话对准备回复的消息进行检查,判断消息是否存在超过记忆之外的东西，是否存在编造
                
                例如：
                1. 准备回复的消息说： 你上次说了给我买奶茶，什么时候给我买啊。 但是历史消息中并没有相关记忆
                2. 准备回复的消息说： 你昨天跟我说要来找我。 但是历史消息中并没有相关记忆。
                                
                历史消息：{memoryString}
                                
                准备回复的消息：{generateMessage}
                                
                输出要求：
                1. 如果不存在超过记忆的回复，就输出  通过
                2. 如果存在超过记忆的回复，就输出  不通过 原因：生成的消息提到上次给我买奶茶，但是记忆中并没有相关内容，存在伪造记忆。
                3. 输出不要超过五十字
                """).build();
        Prompt prompt = promptTemplate.create(Map.of("generateMessage", generateMessage, "memoryString", memoryString));
        log.info("记忆幻想检查提示词：{}", prompt);
        String memoryCheckOutput = chatClient.prompt(prompt)
                .call().content();
        log.info("记忆幻想检查结果输出：{}", memoryCheckOutput);
        return Map.of("memoryCheckOutput", memoryCheckOutput);
    }

}
