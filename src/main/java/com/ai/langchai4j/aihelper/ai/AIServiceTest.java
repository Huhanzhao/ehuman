package com.ai.langchai4j.aihelper.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface AIServiceTest {

    @SystemMessage("我叫{{name}}")
    String chatWithString(@V("name") String name, @UserMessage String message);
}
