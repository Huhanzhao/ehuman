package com.helper.services.bean;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.helper.common.entity.BusinessException;
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
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.Map;

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
            List<TbUserModelConfigDo> modelConfigDos = tbUserModelConfigDoService.list(Wrappers.<TbUserModelConfigDo>lambdaQuery()
                    .eq(TbUserModelConfigDo::getUserId, userId));
            if (modelConfigDos.isEmpty()) throw new BusinessException("请先配置apikey",userId);
            modelConfigDo = modelConfigDos.getFirst();
        }

        TbChatConfigDo chatConfigDo = tbChatConfigDoService.getOne(Wrappers.<TbChatConfigDo>lambdaQuery()
                .eq(TbChatConfigDo::getUserId, userId));

        if (ChatModelEnum.GPT_4O_MINI.getCategory().equals(modelConfigDo.getCategory()) ||
                ChatModelEnum.DEEP_SEEK_V3_2.getCategory().equals(modelConfigDo.getCategory()) ||
                ChatModelEnum.GROK_4_1_NO_REASONING.getCategory().equals(modelConfigDo.getCategory())
        ){
            return createOpenAiChatClient(modelConfigDo,chatConfigDo);
        }else if (ChatModelEnum.CLAUDE_SONNET_4_5.getCategory().equals(modelConfigDo.getCategory())){
            return createClaudeChatClient(modelConfigDo,chatConfigDo);
        }else {
            throw new BusinessException("不支持的模型",userId);
        }
    }
    public ChatClient createOpenAiChatClient(TbUserModelConfigDo modelConfigDo,TbChatConfigDo chatConfigDo) {
        OpenAiApi.Builder openaiBuilder = OpenAiApi.builder()
                .apiKey(modelConfigDo.getApiKey())
                .baseUrl(modelConfigDo.getBaseUrl());
        if (modelConfigDo.getNeedProxy()){
            SimpleClientHttpRequestFactory factory =  new SimpleClientHttpRequestFactory();

            Proxy proxy = new Proxy(
                    Proxy.Type.HTTP,
                    new InetSocketAddress("127.0.0.1", 7890)
            );

            factory.setProxy(proxy);

            RestClient.Builder builder = RestClient.builder()
                    .requestFactory(factory);
            openaiBuilder.restClientBuilder(builder);
        }

        OpenAiApi openAiApi = openaiBuilder.build();
        OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder()
                .model(modelConfigDo.getModel())
                .temperature(chatConfigDo.getTemperature())
                .maxCompletionTokens(1024)
                .extraBody(Map.of("chat_template_kwargs", Map.of("enable_thinking", false)))
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
