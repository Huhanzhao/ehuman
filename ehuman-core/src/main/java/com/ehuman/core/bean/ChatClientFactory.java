package com.ehuman.core.bean;

import com.ehuman.core.utils.SendMessageUtil;
import com.ehuman.dao.entity.TbChatConfigDo;
import com.ehuman.dao.entity.TbUserModelConfigDo;
import com.ehuman.dao.enums.LlmTypeEnum;
import com.ehuman.dao.service.TbChatConfigDoService;
import com.ehuman.dao.service.TbUserModelConfigDoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.Map;

import static com.ehuman.dao.enums.ChatModelCategoryEnum.*;


@Component
@Slf4j
public class ChatClientFactory {

    @Value("${proxy.host:127.0.0.1}")
    private String proxyHost;

    @Value("${proxy.port:7890}")
    private int proxyPort;

    @Autowired
    private TbUserModelConfigDoService tbUserModelConfigDoService;

    @Autowired
    private TbChatConfigDoService tbChatConfigDoService;

    @Autowired
    private SendMessageUtil sendMessageUtil;

    public ChatClient createChatClient(Long userId, LlmTypeEnum llmTypeEnum) {
        TbUserModelConfigDo modelConfigDo = tbUserModelConfigDoService.getByUserIdAndNodeNum(userId, llmTypeEnum);
        // 没有配置某节点模型 随便取一个模型
        if (modelConfigDo == null){
            List<TbUserModelConfigDo> modelConfigDos = tbUserModelConfigDoService.getByUserId(userId);
            if(llmTypeEnum.equals(LlmTypeEnum.MASTER) || llmTypeEnum.equals(LlmTypeEnum.AUXILIARY)){
                modelConfigDo = modelConfigDos.stream()
                        .filter(e -> e.getLlmTypeEnum().equals(LlmTypeEnum.MASTER) || e.getLlmTypeEnum().equals(LlmTypeEnum.AUXILIARY))
                        .findFirst()
                        .orElse(null);
            }
            if (modelConfigDo == null){
                String modelType = "";
                switch (llmTypeEnum){
                    case LlmTypeEnum.MASTER, LlmTypeEnum.AUXILIARY -> modelType = "chat";
                    case LlmTypeEnum.IMAGE_ANALYSIS -> modelType = "image";
                    case LlmTypeEnum.TTS -> modelType = "tts";
                    case LlmTypeEnum.EMBEDDING -> modelType = "embedding";
                }
                sendMessageUtil.sendSystemMsg(userId,"请先配置apikey,缺少" + modelType + "模型");
                log.warn("请先配置apikey,缺少" + modelType + "模型");
                throw new RuntimeException("请先配置apikey");
            }
        }

        TbChatConfigDo chatConfigDo = tbChatConfigDoService.getByUserId(userId);

        if (GPT.getCategory().equals(modelConfigDo.getCategory()) || IMAGE.getCategory().equals(modelConfigDo.getCategory())){
            return createOpenAiChatClient(modelConfigDo,chatConfigDo);
        }else if (CLAUDE.getCategory().equals(modelConfigDo.getCategory())){
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
                    new InetSocketAddress(proxyHost, proxyPort)
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
                    new InetSocketAddress(proxyHost, proxyPort)
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
