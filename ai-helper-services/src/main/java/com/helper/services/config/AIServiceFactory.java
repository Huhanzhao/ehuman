package com.helper.services.config;


import com.helper.services.PersistentChatMemoryStore;
import com.helper.services.service.AIChatService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIServiceFactory {

//    private OpenAiChatModel openAiChatModel;

    @Resource
    private OpenAiStreamingChatModel openAiStreamingChatModel;

    @Resource
    private PersistentChatMemoryStore persistentChatMemoryStore;

    @Bean
    public AIChatService getAIChatService(){

        AIChatService aiChatService = AiServices.builder(AIChatService.class)
//                .chatMemory(chatMemory) // 共享memory实例
                .chatMemoryProvider(id -> MessageWindowChatMemory.builder()
                        .id(id)
                        .maxMessages(20)
                        .chatMemoryStore(persistentChatMemoryStore)
                        .build()) //聊天记忆 持久化按照id查询不同记忆
                .streamingChatModel(openAiStreamingChatModel) // 流式聊天模型
//                .contentRetriever() // 检索器
                .tools() // 工具
//                .toolProvider()
                .build();
        return aiChatService;
    }
}
