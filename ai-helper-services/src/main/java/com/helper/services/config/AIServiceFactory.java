package com.helper.services.config;


import com.helper.services.service.AIChatService;
import com.helper.services.tools.SearchOldMemoryTool;
import com.helper.services.tools.SearchWebTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIServiceFactory {

    @Resource
    private OpenAiStreamingChatModel openAiStreamingChatModel;

    @Resource
    private PersistentChatMemoryStore persistentChatMemoryStore;

    @Resource
    private SearchWebTool searchWebTool;

    @Resource
    private SearchOldMemoryTool searchOldMemoryTool;

    // 持久化记忆聊天服务
    @Bean
    public AIChatService getAIChatService(){
        return AiServices.builder(AIChatService.class)
                .chatMemoryProvider(id -> MessageWindowChatMemory.builder()
                        .id(id)
                        .maxMessages(20)
                        .chatMemoryStore(persistentChatMemoryStore)
                        .build()) //聊天记忆 持久化按照id查询不同记忆
                .streamingChatModel(openAiStreamingChatModel) // 流式聊天模型
                .tools(searchWebTool,searchOldMemoryTool) // 搜索引擎工具 旧记忆RAG检索
                .build();
    }


    // rag服务
    @Bean
    public AIChatService getAIRagService(){

        return AiServices.builder(AIChatService.class)
//                .chatMemory(chatMemory) // 共享memory实例
                .chatMemoryProvider(id -> MessageWindowChatMemory.builder()
                        .id(id)
                        .maxMessages(20)
                        .chatMemoryStore(persistentChatMemoryStore)
                        .build()) //聊天记忆 持久化按照id查询不同记忆
                .streamingChatModel(openAiStreamingChatModel) // 流式聊天模型
//                .contentRetriever() // todo 检索器
                .tools(searchWebTool,searchOldMemoryTool) // 搜索工具 //或者用 .toolProvider() 懒加载
                .build();
    }
}
