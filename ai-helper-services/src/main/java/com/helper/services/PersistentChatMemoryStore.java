package com.helper.services;

import com.ai.langchai4j.aihelper.demand.entity.TbAiChatMemory;
import com.ai.langchai4j.aihelper.demand.service.TbAiChatMemoryService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PersistentChatMemoryStore implements ChatMemoryStore {

        @Autowired
        private TbAiChatMemoryService tbAiChatMemoryService;

        @Override
        public List<ChatMessage> getMessages(Object memoryId) {

                List<ChatMessage> messages = new ArrayList<>();
                List<TbAiChatMemory> tbAiChatMemoriesList =
                        tbAiChatMemoryService.list(Wrappers.<TbAiChatMemory>lambdaQuery()
                        .eq(TbAiChatMemory::getMemoryId, memoryId)
                                .orderByAsc(TbAiChatMemory::getCreateTime));

                // 可以使用ChatMessageDeserializer.messageFromJson(String)和
                // ChatMessageDeserializer.messagesFromJson(String)辅助方法
                // 轻松地从JSON反序列化聊天消息。

                tbAiChatMemoriesList.forEach(param -> {
                        messages.add(ChatMessageDeserializer.messageFromJson(param.getMessage()));
                });
                return messages;
        }

        @Override
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
                ChatMessage chatMessage = messages.get(messages.size() - 1);
                TbAiChatMemory tbAiChatMemory = new TbAiChatMemory();
                tbAiChatMemory.setMemoryId(memoryId.toString());
                tbAiChatMemory.setRole(chatMessage.type().name().toLowerCase());

                // 可以使用ChatMessageSerializer.messageToJson(ChatMessage)和
                // ChatMessageSerializer.messagesToJson(List<ChatMessage>)辅助方法
                // 轻松地将聊天消息序列化为JSON。
                tbAiChatMemory.setMessage(ChatMessageSerializer.messageToJson(chatMessage));
                tbAiChatMemoryService.saveOrUpdate(tbAiChatMemory);
        }

        @Override
        public void deleteMessages(Object memoryId) {
                tbAiChatMemoryService.remove(Wrappers.<TbAiChatMemory>lambdaQuery()
                        .eq(TbAiChatMemory::getMemoryId, memoryId.toString()));
                System.out.println("deleteMessages方法被触发了");
        }
}