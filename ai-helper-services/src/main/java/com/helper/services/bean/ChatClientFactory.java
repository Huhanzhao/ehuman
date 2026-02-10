package com.helper.services.bean;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.helper.services.entity.TbChatConfigDo;
import com.helper.services.enums.ChatModelEnum;
import com.helper.services.service.TbChatConfigDoService;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ChatClientFactory {

    @Autowired
    private TbChatConfigDoService tbChatConfigDoService;

    public ChatClient createChatClient(ChatModelEnum chatModelEnum) {
        if (chatModelEnum == ChatModelEnum.GPT_4O_MINI ||
                chatModelEnum == ChatModelEnum.GPT_5){
            return createOpenAiChatClient(chatModelEnum);
        }else if (chatModelEnum == ChatModelEnum.CLAUDE_SONNET_4_5){
            return createClaudeChatClient(chatModelEnum);
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
                .temperature(0.0)
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

    private ChatClient createOpenAiChatClient(ChatModelEnum chatModelEnum) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey("sk-PLLvztGg0Oa2qxQ5TfkMuU4j7PT5MbUm6Mo7Kaei21HQfffo")
                .baseUrl("https://api.openai-proxy.org").build();

        OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder()
                .model(chatModelEnum.getModelName())
                .temperature(1.0)
                .maxCompletionTokens(1024)
//                .topP(1.0) // 模型选词自由度
//                .presencePenalty(1.0) // 模型换词、换说法
//                .maxTokens(1024)
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

    private ChatClient createClaudeChatClient(ChatModelEnum chatModelEnum) {
        AnthropicApi anthAiApi = AnthropicApi.builder()
                .apiKey("sk-PLLvztGg0Oa2qxQ5TfkMuU4j7PT5MbUm6Mo7Kaei21HQfffo")
                .baseUrl("https://api.openai-proxy.org/anthropic").build();


        TbChatConfigDo chatConfigDo = tbChatConfigDoService.getOne(Wrappers.<TbChatConfigDo>lambdaQuery()
                );
        AnthropicChatOptions openAiChatOptions = AnthropicChatOptions.builder()
                .model(chatModelEnum.getModelName())
                .temperature(chatConfigDo.getTemperature())
                .maxTokens(1024)
//                .topP(chatConfigDo.getTopP()) // 模型选词自由度 claude 不支持和温度一起用
//                .presencePenalty(1.0) // 模型换词、换说法
//                .maxTokens(1024)
                .build();

//        DefaultToolCallingManager toolCallingManager = ToolCallingManager.builder()
//                .build();

        AnthropicChatModel openAiChatModel = AnthropicChatModel.builder()
                .anthropicApi(anthAiApi)
                .defaultOptions(openAiChatOptions)
//                .toolCallingManager(toolCallingManager)
                .build();
        return ChatClient.create(openAiChatModel);
//        return ChatClient.builder(openAiChatModel)
//                .defaultSystem("你是一个对话机器人")
//                .build();
    }
}
