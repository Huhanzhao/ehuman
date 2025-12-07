package com.helper.services.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.helper.config.ChromaEmbeddingStoreFactory;
import com.helper.services.entity.TbAiChatMemory;
import com.helper.services.service.TbAiChatMemoryService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class PersistentChatMemoryStore implements ChatMemoryStore {

        @Autowired
        private TbAiChatMemoryService tbAiChatMemoryService;

        @Autowired
        private OpenAiEmbeddingModel openAiEmbeddingModel;

        @Override
        public List<ChatMessage> getMessages(Object memoryId) {

                List<ChatMessage> messages = new ArrayList<>();
                List<TbAiChatMemory> tbAiChatMemoriesList =
                        tbAiChatMemoryService.list(Wrappers.<TbAiChatMemory>lambdaQuery()
                        .eq(TbAiChatMemory::getMemoryId, memoryId)
                                        .eq(TbAiChatMemory::getVectorized,false)
                                .orderByDesc(TbAiChatMemory::getCreateTime)
                                .last("LIMIT 25"));

                List<TbAiChatMemory> reversed = tbAiChatMemoriesList.reversed();

                // 可以使用ChatMessageDeserializer.messageFromJson(String)和
                // ChatMessageDeserializer.messagesFromJson(String)辅助方法
                // 轻松地从JSON反序列化聊天消息。

                reversed.forEach(param -> {
                        messages.add(ChatMessageDeserializer.messageFromJson(param.getMessage()));
                });

                return messages;
        }

        @Override
        @Transactional
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
                ChatMessage chatMessage = messages.getLast();

                TbAiChatMemory tbAiChatMemory = new TbAiChatMemory();
                tbAiChatMemory.setMemoryId(memoryId.toString());
                tbAiChatMemory.setRole(chatMessage.type().name().toLowerCase());

                // 可以使用ChatMessageSerializer.messageToJson(ChatMessage)和
                // ChatMessageSerializer.messagesToJson(List<ChatMessage>)辅助方法
                // 轻松地将聊天消息序列化为JSON。
                tbAiChatMemory.setMessage(ChatMessageSerializer.messageToJson(chatMessage));
                tbAiChatMemoryService.saveOrUpdate(tbAiChatMemory);

                log.info("消息已持久化");

                // 异步矢量化旧消息
                CompletableFuture.runAsync(()->{

                        List<ChatMessage> messagesToChroma = new ArrayList<>();
                        // 查询二十条之外的消息
                        List<TbAiChatMemory> tbAiChatMemoryListToChroma =
                                tbAiChatMemoryService.list(Wrappers.<TbAiChatMemory>lambdaQuery()
                                        .eq(TbAiChatMemory::getMemoryId, memoryId)
                                        .eq(TbAiChatMemory::getVectorized,false)
                                        .ne(TbAiChatMemory::getRole,"system")
                                        .orderByDesc(TbAiChatMemory::getCreateTime)
                                        .last("LIMIT 1000 OFFSET 20"));

                        tbAiChatMemoryListToChroma.forEach(param -> {
                                param.setVectorized(true);
                                messagesToChroma.add(ChatMessageDeserializer.messageFromJson(param.getMessage()));
                        });
                        // 矢量化
                        ingestMessages(messagesToChroma.reversed(),memoryId.toString());

                        // 标记为已矢量化
                        tbAiChatMemoryService.updateBatchById(tbAiChatMemoryListToChroma);
                        log.info("标记为已矢量化完毕");
                });
        }

        @Override
        public void deleteMessages(Object memoryId) {
                tbAiChatMemoryService.remove(Wrappers.<TbAiChatMemory>lambdaQuery()
                        .eq(TbAiChatMemory::getMemoryId, memoryId.toString()));
                System.out.println("deleteMessages方法被触发了");
        }

        public void ingestMessages(List<ChatMessage> messages,String memoryId){
                ChromaEmbeddingStore chromaEmbeddingStore = ChromaEmbeddingStoreFactory.getChromaEmbeddingStore("memory");
                // 文档切割器 按照换行拆
                DocumentByParagraphSplitter documentByParagraphSplitter = new DocumentByParagraphSplitter(500, 50);
                EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                        .embeddingStore(chromaEmbeddingStore)
                        .embeddingModel(openAiEmbeddingModel)
                        .documentSplitter(documentByParagraphSplitter)
                        .build();

            for (ChatMessage message : messages) {
                // 将文本封装成 Document
                String role = message.type().name().toLowerCase();
                StringBuffer messageText = new StringBuffer();
                if (message instanceof UserMessage userMessage) {
                    Object first = userMessage.contents().getFirst();
                    if (first instanceof TextContent mes) {
                        messageText.append(mes.text());
                    }
                } else if (message instanceof AiMessage aiMessage) {
                    messageText.append(aiMessage.text());
                } else if (message instanceof ToolExecutionResultMessage toolExecutionResultMessage) {
                    messageText.append(toolExecutionResultMessage.text());
                } else {
                    continue;
                }
                Document doc = Document.from(messageText.toString(),
                        Metadata.from(Map.of("userId", memoryId,
                                "createTime", System.currentTimeMillis(), "role", role)));

                ingestor.ingest(doc);
            }
        }
}