package com.bot.service.utils;

import com.alibaba.fastjson2.util.DateUtils;
import com.bot.service.entity.TbAiChatMemoryDo;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.stream.Collectors;

public class MemoryCastUtils {

    public static String memoryCastToString(List<TbAiChatMemoryDo> memoryDoList) {
        StringBuilder result = new StringBuilder();
        memoryDoList.forEach(memoryDo -> {
            result.append("assistant".equals(memoryDo.getRole())? "ai" : "user").append(": ").append(memoryDo.getMessage())
                    .append(" sendTime:").append(DateUtils.format(memoryDo.getCreateTime(), "yyyy-MM-dd HH:mm:ss"))
                    .append("\n");
        });
        return result.toString();
    }

    public static List<Message> memoryCastToMessages(List<TbAiChatMemoryDo> memoryDoList) {
        return memoryDoList.stream().map(memoryDo -> {
            Message message;
            if ("user".equals(memoryDo.getRole())) {
                message =  new UserMessage(memoryDo.getMessage());
            } else {
                message =  new AssistantMessage(memoryDo.getMessage());
            }
            return message;
        }). collect(Collectors.toList());
    }
}
