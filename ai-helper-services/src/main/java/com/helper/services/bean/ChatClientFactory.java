package com.helper.services.bean;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.helper.commonservicescommon.entity.BusinessException;
import com.helper.services.entity.TbChatConfigDo;
import com.helper.services.entity.TbUserModelConfigDo;
import com.helper.services.enums.ChatModelEnum;
import com.helper.services.enums.NodeEnum;
import com.helper.services.service.TbChatConfigDoService;
import com.helper.services.service.TbUserModelConfigDoService;
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

import java.util.Objects;

@Component
public class ChatClientFactory {

    @Autowired
    private TbUserModelConfigDoService tbUserModelConfigDoService;

    @Autowired
    private TbChatConfigDoService tbChatConfigDoService;

    public ChatClient createChatClient(Long userId, NodeEnum nodeEnum) {
        TbUserModelConfigDo modelConfigDo = tbUserModelConfigDoService.getOne(Wrappers.<TbUserModelConfigDo>lambdaQuery()
                .eq(TbUserModelConfigDo::getUserId, userId).eq(TbUserModelConfigDo::getNodeNum, nodeEnum.getNodeNum()));
        // 没有配置某节点模型 随便取一个模型
        if (modelConfigDo == null){
            modelConfigDo = tbUserModelConfigDoService.getOne(Wrappers.<TbUserModelConfigDo>lambdaQuery()
                    .eq(TbUserModelConfigDo::getUserId, userId));
        }

        TbChatConfigDo chatConfigDo = tbChatConfigDoService.getOne(Wrappers.<TbChatConfigDo>lambdaQuery()
                .eq(TbChatConfigDo::getUserId, userId));

        if (ChatModelEnum.GPT_4O_MINI.getCategory().equals(modelConfigDo.getCategory())){
            return createOpenAiChatClient(modelConfigDo,chatConfigDo);
        }else if (ChatModelEnum.CLAUDE_SONNET_4_5.getCategory().equals(modelConfigDo.getCategory())){
            return createClaudeChatClient(modelConfigDo,chatConfigDo);
        }else if (ChatModelEnum.DEEP_SEEK_V3_2.getCategory().equals(modelConfigDo.getCategory())){
            return createDeepSeekChatClient(modelConfigDo,chatConfigDo);
        }else {
            throw new BusinessException("不支持的模型");
        }
    }

    public ChatClient createDeepSeekChatClient(TbUserModelConfigDo modelConfigDo,TbChatConfigDo chatConfigDo) {
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .apiKey(modelConfigDo.getApiKey())
                .baseUrl(modelConfigDo.getBaseUrl()).build();

        DeepSeekChatOptions deepSeekChatOptions = DeepSeekChatOptions.builder()
                .model(modelConfigDo.getModel())
                .temperature(chatConfigDo.getTemperature())
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
    }

    public ChatClient createOpenAiChatClient(TbUserModelConfigDo modelConfigDo,TbChatConfigDo chatConfigDo) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(modelConfigDo.getApiKey())
                .baseUrl(modelConfigDo.getBaseUrl()).build();

        OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder()
                .model(modelConfigDo.getModel())
                .temperature(chatConfigDo.getTemperature())
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
    }

    public ChatClient createClaudeChatClient(TbUserModelConfigDo modelConfigDo,TbChatConfigDo chatConfigDo) {
        AnthropicApi anthAiApi = AnthropicApi.builder()
                .apiKey(modelConfigDo.getApiKey())
                .baseUrl(modelConfigDo.getBaseUrl()).build();
//        AnthropicApi anthAiApi = AnthropicApi.builder()
//                .apiKey("sk-PLLvztGg0Oa2qxQ5TfkMuU4j7PT5MbUm6Mo7Kaei21HQfffo")
//                .baseUrl("https://api.openai-proxy.org/anthropic").build();



        AnthropicChatOptions openAiChatOptions = AnthropicChatOptions.builder()
                .model(modelConfigDo.getModel())
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
    }
}
