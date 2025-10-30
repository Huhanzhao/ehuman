package com.ai.langchai4j.aihelper.aistudy;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

public interface MyAIService {
    // 聊天对话 List<ChatMessage> -> ChatResponse  多条消息产生“记忆”
    String chatWithMessage(List<ChatMessage> message);

    String chatWithMessage(String message);
}
