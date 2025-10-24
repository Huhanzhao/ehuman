package com.ai.langchai4j.aihelper.ai;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SummaryService {
    private final OpenAiChatModel summarizerModel;

    public SummaryService(
            @Value("${langchain4j.open-ai.chat-model.api-key}") String apiKey,
            @Value("${langchain4j.open-ai.chat-model.base-url}") String baseUrl

    ) {
        // 这里直接创建一个独立的模型，不走 @AiService
        this.summarizerModel = OpenAiChatModel.builder()
                .apiKey(apiKey) // 或者从配置中读取
                .baseUrl(baseUrl)
                .modelName("gpt-4o-mini") // 你自己的模型
                .build();
    }

    public String summarize(String text) {
        return summarizerModel.chat("总结以下消息: " + text);
    }
}