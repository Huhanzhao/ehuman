package com.ai.langchai4j.aihelper.ai;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class AICodeServiceImpl implements AICodeService{

    @Autowired
    private OpenAiChatModel openAiChatModel;

    @Override
    public ChatResponse chatWithMessage(List<ChatMessage> messageList) {
        ChatResponse chatResponse = openAiChatModel.chat(messageList);
        return chatResponse;
    }

    @Override
    public ChatResponse chatWithRequest(List<ChatMessage> messageList) {
        // 创建一个工具
//        ToolSpecification.builder().description("搜索天气").name();

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(messageList)
                .temperature(0.0)  // 温度，越小越 deterministic
//                .toolSpecifications(toolSpecifications)  // todo 模拟调用工具
                .build();
        ChatResponse chatResponse = openAiChatModel.chat(chatRequest);
        return chatResponse;
    }

    @Override
    public ChatResponse chatWithImageAndAudio(String message, String url) {
        UserMessage userMessage = UserMessage.from(TextContent.from(message), ImageContent.from(url));
        ChatResponse chatResponse = openAiChatModel.chat(userMessage);
        return chatResponse;
    }

    @Override
    public ChatResponse chatWithMemoryMessages(List<ChatMessage> messageList) {
        // 创建记忆，最多二十条消息的模式
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(20);
        chatMemory.add(messageList);


        // 创建记忆，按照token的数量限制
        ChatMemory chatMemory2 = TokenWindowChatMemory
                .withMaxTokens(200,new OpenAiTokenCountEstimator("gpt-4o-mini"));
        chatMemory2.add(messageList);

        MyAIService myAIService =
                AiServices.builder(MyAIService.class)
                .chatMemory(chatMemory)
                        .chatModel(openAiChatModel)
                        .build();
        ChatResponse chatResponse = myAIService.chatWithMessage(List.of(messageList.getLast()));
        return chatResponse;
    }

    @Override
    public ChatResponse chatWithMemoryMessagesPersistence(List<ChatMessage> messageList) {

        MessageWindowChatMemory windowChatMemory = MessageWindowChatMemory
                .builder()
                .id("persistence")
                .maxMessages(20)
                .chatMemoryStore(new PersistentChatMemoryStore())
                .build();

        MyAIService myAIService = AiServices.builder(MyAIService.class).chatModel(openAiChatModel)
                .chatMemory(windowChatMemory).build();

        ChatResponse chatResponse = myAIService.chatWithMessage(List.of(messageList.getLast()));
        return chatResponse;
    }

    @Override
    public String chatWithString(String message) {
        String chat = openAiChatModel.chat(message);
        return chat;
    }
}
