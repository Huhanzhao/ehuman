package com.bot.service.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.bot.service.bean.ChatClientFactory;
import com.bot.service.enums.NodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class RealLifeInteractionCheckNode implements NodeAction {

    private final ChatClientFactory chatClientFactory;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        // todo 如果加入了历史消息和总结消息，也要在此节点加入
        Long userId = (Long) state.value("userId").orElse(0L);
        String generateMessage = (String) state.value("generateMessage").orElse("");
        String memoryString = state.value("memoryString", String.class).orElse("");
        String currentActivity = state.value("currentActivity", String.class).orElse("");

        ChatClient chatClient = chatClientFactory.createChatClient(userId, NodeEnum.GENERATE);

        PromptTemplate promptTemplate = PromptTemplate.builder().template("""
                你是消息审核专家。 已知ai只能和用户发消息，不能现实生活中互动,也不能发送图片，可以发送表情包
                **你的任务**是根据历史消息对话对准备回复的消息进行检查,判断准备回复的消息是否存在设计现实生活互动的消息
                
                例如：
                1. 准备回复的消息说： 在楼下等你。 （产生了现实生活互动）
                2. 准备回复的消息说： 你的饭看着真好吃，给我吃一口。（产生了现实生活互动）
                3. 准备回复的消息说： 快回来，我在等你呢。（产生了现实生活互动）
                4. 准备回复的消息说： 你看，今天的天空好美，你看，这张照片拍的真好看。（无法发送照片，不能让用户看，产生了现实生活互动）
                
                本轮对话记忆包括：
                1. **历史消息**：{memoryString}
                2. **ai当前虚拟生活细节**：{currentActivity}
                                
                准备回复的消息：{generateMessage}
                                
                输出要求：
                1. 如果不存在现实生活中互动，就输出  通过
                2. 如果存在现实生活中互动，就输出  不通过 原因：生成的消息说要等用户下楼，产生了现实生活互动。
                3. 输出不要超过三十字
                """).build();
        Prompt prompt = promptTemplate.create(Map.of("generateMessage", generateMessage,
                "memoryString", memoryString,
                "currentActivity", currentActivity
                )
        );
        log.info("现实生活互动检查提示词：{}", prompt);
        String realLifeInteractionCheckOutput = chatClient.prompt(prompt)
                .call().content();
        log.info("现实生活互动结果输出：{}", realLifeInteractionCheckOutput);
        return Map.of("realLifeInteractionCheckOutput", realLifeInteractionCheckOutput);
    }

}
