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
@Slf4j
@Component
public class RoleReversalCheckNode implements NodeAction {

    private final ChatClientFactory chatClientFactory;
    @Override
    public Map<String, Object> apply(OverAllState state) {
        Long userId = (Long)state.value("userId").orElse(0L);
        String generateMessage = (String)state.value("generateMessage").orElse("");
        String memoryString = state.value("memoryString", String.class).orElse("");

        ChatClient chatClient = chatClientFactory.createChatClient(userId, NodeEnum.GENERATE);

        PromptTemplate promptTemplate = PromptTemplate.builder().template("""
                你是消息审核专家，你需要审核ai生成的消息，根据对话历史 判断ai是否把角色弄混乱了。
                比如：
                例子1：历史对话里面是用户没有搭理ai，ai却说自己没有搭理用户
                例子2：历史对话里面是ai说以后不要再理用户了，但是ai生成的消息却说："你刚刚说不理我...."
                
                历史消息：{memoryString}
                
                准备回复的消息：{generateMessage}
                
                输出要求：
                1. 如果不存在角色混乱的问题，就输出 通过
                2. 如果存在角色混乱的问题，就输出 不通过 原因：xxx （例如：不正常 角色混乱，生成的消息ai认错说不应该不理用户，但是历史记录中是用户不理ai）
                3. 输出不要超过五十字
                """).build();
        Prompt prompt = promptTemplate.create(Map.of("generateMessage",generateMessage,"memoryString",memoryString));
        log.info("角色混乱审核提示词：{}" , prompt);
        String roleReversalCheckOutput = chatClient.prompt(prompt)
                .call().content();
        log.info("角色混乱审核结果输出：{}" , roleReversalCheckOutput);
        return Map.of("roleReversalCheckOutput", roleReversalCheckOutput);
    }

}
