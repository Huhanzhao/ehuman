package com.helper.services.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import reactor.core.publisher.Flux;

public interface AIChatService {

    @SystemMessage(fromResource = "ragChatPrompt.txt")
    Flux<String> chatWithString(@MemoryId String memoryId, @UserMessage String message,@V("userId") String userId);
}
