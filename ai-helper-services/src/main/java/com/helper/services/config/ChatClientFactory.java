package com.helper.services.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.helper.services.enums.ChatModelEnum;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ChatClientFactory {

    public ChatClient createChatClient(ChatModelEnum chatModelEnum) {
        if (chatModelEnum == ChatModelEnum.GPT_4O_MINI){
            return createOpenAiChatClient();
        }else {
            return createDeepSeekChatClient();
        }
    }

    private ChatClient createDeepSeekChatClient() {
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .apiKey("sk-4111b047ff9e4752bd17509c30859e89")
                .baseUrl("https://api.deepseek.com").build();

        DeepSeekChatOptions deepSeekChatOptions = DeepSeekChatOptions.builder()
                .model(ChatModelEnum.DEEP_SEEK_V3_2.getModelName())
                .temperature(0.7)
                .maxTokens(1024)
                .build();

//        DefaultToolCallingManager toolCallingManager = ToolCallingManager.builder()
//                .build();

        DeepSeekChatModel deepSeekChatModel = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .defaultOptions(deepSeekChatOptions)
//                .toolCallingManager(toolCallingManager)
                .build();


        return ChatClient.create(deepSeekChatModel);
//         ChatClient.builder(deepSeekChatModel)
//                .defaultSystem("你是一个对话机器人")
//                .build();
    }

    private ChatClient createOpenAiChatClient() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey("sk-PLLvztGg0Oa2qxQ5TfkMuU4j7PT5MbUm6Mo7Kaei21HQfffo")
                .baseUrl("https://api.openai-proxy.org").build();

        OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder()
                .model(ChatModelEnum.GPT_4O_MINI.getModelName())
                .temperature(0.7)
                .maxTokens(1024)
                .build();

//        DefaultToolCallingManager toolCallingManager = ToolCallingManager.builder()
//                .build();

        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(openAiChatOptions)
//                .toolCallingManager(toolCallingManager)
                .build();
        return ChatClient.create(openAiChatModel);
//        return ChatClient.builder(openAiChatModel)
//                .defaultSystem("你是一个对话机器人")
//                .build();
    }
}
