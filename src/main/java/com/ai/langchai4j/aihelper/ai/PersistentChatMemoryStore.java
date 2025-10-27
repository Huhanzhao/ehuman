package com.ai.langchai4j.aihelper.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.List;

public class PersistentChatMemoryStore implements ChatMemoryStore {

        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
          // TODO: 实现通过内存ID从持久化存储中获取所有消息。
          // 可以使用ChatMessageDeserializer.messageFromJson(String)和
          // ChatMessageDeserializer.messagesFromJson(String)辅助方法
          // 轻松地从JSON反序列化聊天消息。
                return List.of(SystemMessage.from("你的名字叫小红"),
                        UserMessage.from("你好，你知道我叫什么吗"),
                        AiMessage.from("我不知道"),
                        UserMessage.from("我叫小明"),AiMessage.from("好的，我知道了"));
        }

        @Override
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
            // TODO: 实现通过内存ID更新持久化存储中的所有消息。
            // 可以使用ChatMessageSerializer.messageToJson(ChatMessage)和
            // ChatMessageSerializer.messagesToJson(List<ChatMessage>)辅助方法
            // 轻松地将聊天消息序列化为JSON。
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