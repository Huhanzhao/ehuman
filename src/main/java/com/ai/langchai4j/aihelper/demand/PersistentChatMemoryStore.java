package com.ai.langchai4j.aihelper.demand;

import com.ai.langchai4j.aihelper.demand.entity.TbAiChatMemory;
import com.ai.langchai4j.aihelper.demand.service.TbAiChatMemoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PersistentChatMemoryStore implements ChatMemoryStore {

        @Autowired
        private TbAiChatMemoryService tbAiChatMemoryService;

        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
                List<String> messages =
                        tbAiChatMemoryService.list(Wrappers.<TbAiChatMemory>lambdaQuery()
                        .eq(TbAiChatMemory::getId, memoryId)
                                .orderByAsc(TbAiChatMemory::getCreateTime)).stream()
                                .map(TbAiChatMemory::getMessage).toList();
                return messages;

                // 可以使用ChatMessageDeserializer.messageFromJson(String)和
                // ChatMessageDeserializer.messagesFromJson(String)辅助方法
                // 轻松地从JSON反序列化聊天消息。
        }

        @Override
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
            // TODO: 实现通过内存ID更新持久化存储中的所有消息。
            // 可以使用ChatMessageSerializer.messageToJson(ChatMessage)和
            // ChatMessageSerializer.messagesToJson(List<ChatMessage>)辅助方法
            // 轻松地将聊天消息序列化为JSON。
                System.out.println("一共 " + messages.size() + " 条消息"  );
                System.out.println(messages.toString());
                System.out.println(memoryId);
                System.out.println("updateMessages方法被触发了");
        }

        @Override
        public void deleteMessages(Object memoryId) {
          // TODO: 实现通过内存ID删除持久化存储中的所有消息。
                System.out.println("deleteMessages方法被触发了");
        }
}