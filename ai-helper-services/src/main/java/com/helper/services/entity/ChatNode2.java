package com.helper.services.entity;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.helper.services.config.ChatClientFactory;
import com.helper.services.enums.ChatModelEnum;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class ChatNode2 implements AsyncNodeAction {

    private final   ChatClientFactory chatClientFactory;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        Integer userMessage = (Integer)state.value("input").orElse(0);
        System.out.println("node2userMessage:" + userMessage);
//        ChatClient chatClient = chatClientFactory.createChatClient(ChatModelEnum.GPT_4O_MINI);
//        String content = chatClient.prompt().user("你知道我是谁吗").call().content();
        return CompletableFuture.completedFuture(Map.of("output", "node2结果"));
    }

    public static String decideNextNode(OverAllState state) {
        // 你可以在这里使用 `state` 来判断条件
        Integer someCondition = (Integer) state.value("input").orElse(0);

        if (someCondition > 3) {
            return "node3";  // 如果条件满足，返回 node1
        } else {
            return "node1";  // 否则返回 node3
        }
    }
}
