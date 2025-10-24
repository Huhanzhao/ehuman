package com.ai.langchai4j.aihelper.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface AiServiceInterface {
    @SystemMessage(fromResource = "systemPrompt.txt")
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);


    @SystemMessage("你是一个非常善于总结对话消息的助手，你会总结消息的重点内容，总结的很精简并且不会忽略重点")
    String summaryMessage(@UserMessage String userMessage);
}
