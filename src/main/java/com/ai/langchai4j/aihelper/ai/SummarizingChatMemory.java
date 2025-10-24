package com.ai.langchai4j.aihelper.ai;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.service.MemoryId;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;

@Data
public class SummarizingChatMemory implements ChatMemory {

    private final SummaryService summaryService;


    private final ChatMemory delegate;

    private int maxMessagesBeforeSummarize = 20;

    private String memoryId;

    public SummarizingChatMemory(String memoryId, ChatMemory delegate,int maxMessagesBeforeSummarize, SummaryService summaryService) {
        this.delegate = delegate;
        this.memoryId = memoryId;
        this.maxMessagesBeforeSummarize = maxMessagesBeforeSummarize;
        this.summaryService = summaryService;
    }

    @Override
    public Object id() {
        return memoryId;
    }

    @Override
    public void add(ChatMessage message) {
        delegate.add(message);
        if (delegate.messages().size() > maxMessagesBeforeSummarize) {
            String summary = summaryService.summarize(delegate.messages().toString());
            delegate.clear();
            // 异步生成摘要
//            Executors.newSingleThreadExecutor().submit(() -> {
                String sumMessage = summaryService.summarize(summary);
                delegate.add(new SystemMessage("摘要记忆：" + sumMessage));
//            });
        }
    }

    @Override
    public List<ChatMessage> messages() {
        return delegate.messages();
    }


    // 清空记忆
    @Override
    public void clear() {
        delegate.clear();
    }
}