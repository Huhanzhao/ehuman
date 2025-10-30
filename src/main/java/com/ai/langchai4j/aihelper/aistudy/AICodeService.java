package com.ai.langchai4j.aihelper.aistudy;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.List;

public interface AICodeService {

    // 简单对话 String -> String
    String chatWithString(String message);

    // 聊天对话 List<ChatMessage> -> ChatResponse  多条消息产生“记忆”
    ChatResponse chatWithMessage(List<ChatMessage> message);

    // 更高自由度调节聊天温度参数 List<ChatMessage> -> ChatResponse  实现使用requestbuilder
    ChatResponse chatWithRequest(List<ChatMessage> message);

    // 多模态 图片与音频
    ChatResponse chatWithImageAndAudio(String message, String url);


    // 聊天与记忆
    // MessageWindowChatMemory 与 TokenWindowChatMemory
    String chatWithMemoryMessages(List<ChatMessage> messageList);

    // 持久化聊天记录
    String chatWithMemoryMessagesPersistence(List<ChatMessage> messageList);


}
