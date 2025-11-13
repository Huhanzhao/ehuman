package com.helper.services.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface AIChatService {

    @SystemMessage(fromResource = "aiChatPrompt.txt")
    Flux<String> chatWithString(@MemoryId String memoryId, @UserMessage String message);
}
