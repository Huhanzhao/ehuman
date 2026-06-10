package com.bot.service.bean;

import com.bot.service.entity.TbChatConfigDo;
import com.bot.service.entity.TbUserModelConfigDo;
import com.bot.service.enums.ChatModelEnum;
import com.bot.service.enums.NodeEnum;
import com.bot.service.service.TbChatConfigDoService;
import com.bot.service.service.TbUserModelConfigDoService;
import com.bot.service.utils.SendMessageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.ChatClient;
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
@Slf4j
public class ChatClientFactory {

    @Autowired
    private TbUserModelConfigDoService tbUserModelConfigDoService;

    @Autowired
    private TbChatConfigDoService tbChatConfigDoService;

    @Autowired
    private SendMessageUtil sendMessageUtil;

    public ChatClient createChatClient(Long userId, NodeEnum nodeEnum) {
        TbUserModelConfigDo modelConfigDo = tbUserModelConfigDoService.getByUserIdAndNodeNum(userId, nodeEnum);
        // 没有配置某节点模型 随便取一个模型
        if (modelConfigDo == null){
            List<TbUserModelConfigDo> modelConfigDos = tbUserModelConfigDoService.getByUserId(userId);
            if (modelConfigDos.isEmpty()){
                log.warn("请先配置apikey");
                sendMessageUtil.sendMegToSelf("请先配置apikey");
                throw new RuntimeException("请先配置apikey");
            }
            modelConfigDo = modelConfigDos.getFirst();
        }

        TbChatConfigDo chatConfigDo = tbChatConfigDoService.getByUserId(userId);

        if (ChatModelEnum.GPT_4O_MINI.getCategory().equals(modelConfigDo.getCategory()) ||
                ChatModelEnum.DEEP_SEEK_V3_2.getCategory().equals(modelConfigDo.getCategory()) ||
                ChatModelEnum.GROK_4_1_NO_REASONING.getCategory().equals(modelConfigDo.getCategory())
        ){
            return createOpenAiChatClient(modelConfigDo,chatConfigDo);
        }else if (ChatModelEnum.CLAUDE_SONNET_4_5.getCategory().equals(modelConfigDo.getCategory())){
            return createClaudeChatClient(modelConfigDo,chatConfigDo);
        }else {
            log.error("不支持的模型");
            sendMessageUtil.sendMegToSelf("不支持的模型");
            throw new RuntimeException("不支持的模型");
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
                .temperature(chatConfigDo == null? 0.5:chatConfigDo.getTemperature())
                .maxCompletionTokens(1024)
                .extraBody(Map.of("thinking", Map.of("type", "enabled")))
                // {"thinking": {"type": "enabled/disabled"}}
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
        AnthropicApi.Builder authropicApiBuilder = AnthropicApi.builder()
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
            authropicApiBuilder.restClientBuilder(builder);
        }
        AnthropicApi anthAiApi = authropicApiBuilder.build();

        AnthropicChatOptions openAiChatOptions = AnthropicChatOptions.builder()
                .model(modelConfigDo.getModel())
                .temperature(chatConfigDo == null? 0.5:chatConfigDo.getTemperature())
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
