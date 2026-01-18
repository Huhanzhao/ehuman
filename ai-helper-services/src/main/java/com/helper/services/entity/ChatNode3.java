package com.helper.services.entity;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.helper.services.config.ChatClientFactory;
import com.helper.services.enums.ChatModelEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class ChatNode3 implements AsyncNodeAction {

    private final   ChatClientFactory chatClientFactory;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        Integer userMessage = (Integer)state.value("input").orElse(0);
        System.out.println("node3userMessage:" + userMessage);
//        ChatClient chatClient = chatClientFactory.createChatClient(ChatModelEnum.GPT_4O_MINI);
//        String content = chatClient.prompt().user("你知道我是谁吗").call().content();
        System.out.println("节点三结果执行完毕");
        return CompletableFuture.completedFuture(Map.of("output", "节点三结果"));
    }
}
