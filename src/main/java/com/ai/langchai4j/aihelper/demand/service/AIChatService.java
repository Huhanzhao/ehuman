package com.ai.langchai4j.aihelper.demand.service;

import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

public interface AIChatService {

    @SystemMessage("你是一个聊天助手")
    Flux<String> chatWithString(String message);
}
