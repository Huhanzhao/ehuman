package com.ai.langchai4j.aihelper.ai;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class AiMemoryConfig {

    private final ConcurrentHashMap<String, ChatMemory> memoryMap = new ConcurrentHashMap<>();

    private final SummaryService summaryService;

    public AiMemoryConfig(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> memoryMap.computeIfAbsent((String) memoryId, id -> {
            // 用普通滑动窗口作为底层 memory
            ChatMemory base = MessageWindowChatMemory.withMaxMessages(20);

            // 检查 memory 是否已经有系统提示词（非摘要）
            boolean hasPrompt = base.messages().stream()
                    .filter(m -> m instanceof SystemMessage)
                    .noneMatch(m -> ((SystemMessage) m).text().startsWith("摘要记忆："));

            if (!hasPrompt) {
                // 只在没有摘要的情况下插入提示词
                base.add(new SystemMessage(readSystemPrompt()));
            }


            // 外层包装成带摘要功能的 memory
            return new SummarizingChatMemory(id, base,3,summaryService);
        });
    }

    private String readSystemPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource("systemPrompt.txt");
            return Files.readString(resource.getFile().toPath());
        } catch (Exception e) {
            throw new RuntimeException("读取 systemPrompt.txt 出错", e);
        }
    }
}