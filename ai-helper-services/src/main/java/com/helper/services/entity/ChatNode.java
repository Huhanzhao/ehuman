package com.helper.services.entity;

import ch.qos.logback.core.rolling.DefaultTimeBasedFileNamingAndTriggeringPolicy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
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
public class ChatNode implements AsyncNodeAction {

    private final   ChatClientFactory chatClientFactory;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        Integer userMessage = (Integer)state.value("input").orElse(0);
        System.out.println("node1userMessage:" + userMessage);
//        ChatClient chatClient = chatClientFactory.createChatClient(ChatModelEnum.GPT_4O_MINI);
//        String content = chatClient.prompt().user(userMessage).call().content();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
//        System.out.println("node:content:" + content);
        return CompletableFuture.completedFuture(Map.of("input", userMessage  + 1));
    }
}
