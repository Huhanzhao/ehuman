//package com.ai.langchai4j.aihelper.ai;
//import dev.langchain4j.memory.chat.MessageWindowChatMemory;
//import dev.langchain4j.model.chat.ChatModel;
//import dev.langchain4j.service.AiServices;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class AiServiceInterfaceFactory {
//
//    @Autowired
//    private ChatModel openAiChatModel;
//
//    // AiServiceInterface的工厂类，用来创建返回一个实现了接口的实例
//    @Bean
//    public AiServiceInterface createAiServiceInterface(){
//        // 创建记忆
//        MessageWindowChatMemory recentMemory = MessageWindowChatMemory.withMaxMessages(20);
//
//        // 创建带有记忆的AIService
//        return AiServices.builder(AiServiceInterface.class)
//                .chatMemory(recentMemory)
//                .chatModel(openAiChatModel)
//                .build();
//
//        // todo 绑定工具
//
//
//        // todo rag
//
//
//        // todo 记忆持久化
//
//
//        // todo 图像输入 图像生成
//
//    }
//}
