package com.ai.langchai4j.aihelper.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiServiceInterfaceFactory {

    @Autowired
    private ChatModel openAiChatModel;

    @Bean
    public AiServiceInterface createAiServiceInterface(){
        AiServiceInterface aiServiceInterface = AiServices.create(AiServiceInterface.class, openAiChatModel);
        return aiServiceInterface;
    }
}
