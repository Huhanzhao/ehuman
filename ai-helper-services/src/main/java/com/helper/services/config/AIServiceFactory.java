package com.helper.services.config;


import com.helper.services.service.AIChatService;
import com.helper.services.tools.SearchKnowledgeTool;
import com.helper.services.tools.SearchOldMemoryTool;
import com.helper.services.tools.SearchWebTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Slf4j
public class AIServiceFactory {

    @Resource
    private OpenAiChatModel  openAiChatModel;

    @Resource
    private OpenAiStreamingChatModel openAiStreamingChatModel;

    @Resource
    private PersistentChatMemoryStore persistentChatMemoryStore;

    @Resource
    private SearchWebTool searchWebTool;

    @Resource
    private SearchOldMemoryTool searchOldMemoryTool;

    @Autowired
    private ContentRetriever contentRetriever;

    @Autowired
    private SearchKnowledgeTool searchKnowledgeTool;

    // 持久化记忆聊天服务 agent 自主选择检索记忆和资料库
    @Bean
    @Qualifier("AIChatService")
    public AIChatService getAIChatService(){
        return AiServices.builder(AIChatService.class)
                .chatMemoryProvider(id -> MessageWindowChatMemory.builder()
                        .id(id)
                        .maxMessages(25)
                        .chatMemoryStore(persistentChatMemoryStore)
                        .build()) //聊天记忆 持久化按照id查询不同记忆
                .streamingChatModel(openAiStreamingChatModel) // 流式聊天模型
                .tools(searchWebTool,searchOldMemoryTool,searchKnowledgeTool) // 搜索引擎工具 旧记忆RAG检索 添加检索资料库的工具
                .build();
    }

    @Bean
    @Qualifier("AIChatModelService")
    public AIChatService getAIChatModelService(){
        return AiServices.builder(AIChatService.class)
                .chatMemoryProvider(id -> MessageWindowChatMemory.builder()
                        .id(id)
                        .maxMessages(25)
                        .chatMemoryStore(persistentChatMemoryStore)
                        .build()) //聊天记忆 持久化按照id查询不同记忆
                .chatModel(openAiChatModel) // 非流式聊天模型
                .tools(searchWebTool,searchOldMemoryTool,searchKnowledgeTool) // 搜索引擎工具 旧记忆RAG检索 添加检索资料库的工具
                .build();
    }


    // 专业rag服务 直接绑定了检索器 每次都会检索资料库
    @Bean
    @Qualifier("AIRAGChatService")
    public AIChatService getAIRagService(){

        return AiServices.builder(AIChatService.class)
//                .chatMemory(chatMemory) // 共享memory实例
                .chatMemoryProvider(id -> MessageWindowChatMemory.builder()
                        .id(id)
                        .maxMessages(25)
                        .chatMemoryStore(persistentChatMemoryStore)
                        .build()) //聊天记忆 持久化按照id查询不同记忆
                .streamingChatModel(openAiStreamingChatModel) // 流式聊天模型
                .contentRetriever(query -> {
                    log.info("【RAG 检索触发】查询内容：{}", query);
                    List<Content> retrieve = contentRetriever.retrieve(query);
                    log.info("【RAG 检索结果】共 {} 条", retrieve.size());
                    return retrieve;
                }) // 检索器
                .tools(searchWebTool,searchOldMemoryTool) // 搜索工具 //或者用 todo .toolProvider() 懒加载  高级用法待探索
                .build();
    }
}
