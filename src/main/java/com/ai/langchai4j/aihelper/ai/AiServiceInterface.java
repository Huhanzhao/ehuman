package com.ai.langchai4j.aihelper.ai;

import dev.langchain4j.service.SystemMessage;
import org.springframework.beans.factory.annotation.Autowired;

public interface AiServiceInterface {
    @SystemMessage(fromResource = "systemPrompt.txt")
    String chat(String input);
}
