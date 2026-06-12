package com.anyfriend.core.utils;

import com.anyfriend.dao.entity.TbAiChatMemoryDo;
import org.springframework.ai.chat.messages.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class MemoryCastUtils {

    public static String memoryCastToString(List<TbAiChatMemoryDo> memoryDoList) {
        List<TbAiChatMemoryDo> tbAiChatMemoryDos = fillSystemTimeMessage(memoryDoList);
        StringBuilder result = new StringBuilder();
        tbAiChatMemoryDos.forEach(memoryDo -> {
            result.append(memoryDo.getRole()).append(" : ").append(memoryDo.getMessage())
//                    .append(" sendTime:").append(DateUtils.format(memoryDo.getCreateTime(), "yyyy-MM-dd HH:mm:ss"))
                    .append("\n");
        });
        return result.toString();
    }

    public static List<Message> memoryCastToMessages(List<TbAiChatMemoryDo> memoryDoList) {
        List<TbAiChatMemoryDo> tbAiChatMemoryDos = fillSystemTimeMessage(memoryDoList);
        return tbAiChatMemoryDos.stream().map(memoryDo -> {
            Message message;
            if (MessageType.USER.getValue().equals(memoryDo.getRole())) {
                message =  new UserMessage(memoryDo.getMessage());
            } else if (MessageType.ASSISTANT.getValue().equals(memoryDo.getRole())){
                message =  new AssistantMessage(memoryDo.getMessage());
            }else {
                message =  new SystemMessage(memoryDo.getMessage());
            }
            return message;
        }).collect(Collectors.toList());
    }

    public static List<TbAiChatMemoryDo> fillSystemTimeMessage(List<TbAiChatMemoryDo> memoryDoList) {
        if (memoryDoList.isEmpty()) return new ArrayList<>();

        // 先看有没有user消息  如果没有 加入旁白 距离上次消息 多久 用户一直没有回复你的消息
        if (memoryDoList.stream().noneMatch(memoryDo -> MessageType.USER.getValue().equals(memoryDo.getRole()))){
            Date lastMessageTime = memoryDoList.getLast().getCreateTime();
            String timeAgo = getTimeAgo(lastMessageTime);
            memoryDoList.add(new TbAiChatMemoryDo().setRole(MessageType.SYSTEM.getValue())
                    .setMessage("[旁白：距离上次消息 " + timeAgo + " 用户一直没有回复你的消息]"));
            return memoryDoList;
        }

        // 如果有 查询最近一次ai消息时间 忽略user消息 加入旁白 距离上次消息多久
        for (int i = memoryDoList.size() - 1; i >= 0; i--){
            // 出现ai消息大于五分钟放入旁白
            if (MessageType.ASSISTANT.getValue().equals(memoryDoList.get(i).getRole())){

                Date lastUserMessageTime = memoryDoList.get(i).getCreateTime();
                if (lastUserMessageTime.getTime() < System.currentTimeMillis() - 5 * 60 * 1000){
                    String timeAgo = getTimeAgo(lastUserMessageTime);
                    // 插入到这条 AI 消息的后面
                    TbAiChatMemoryDo tbAiChatMemoryDo = new TbAiChatMemoryDo()
                            .setRole(MessageType.SYSTEM.getValue())
                            .setMessage("[旁白：过了 " + timeAgo + "]");
                    memoryDoList.add(i + 1, tbAiChatMemoryDo);
                }
                break;
            }
        }
        return memoryDoList;
    }

    public static String getTimeAgo(Date lastMessageTime) {
        long now = System.currentTimeMillis();
        long diff = now - lastMessageTime.getTime();  // 毫秒差值

        // 如果时间是未来（异常情况），直接返回
        if (diff < 0) {
            return "刚刚";
        }

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days >= 1) {
            // 超过一天：显示 "1天3小时30分钟"
            long remainHours = hours % 24;
            long remainMinutes = minutes % 60;

            if (remainHours > 0 && remainMinutes > 0) {
                return days + "天" + remainHours + "小时" + remainMinutes + "分钟";
            } else if (remainHours > 0) {
                return days + "天" + remainHours + "小时";
            } else if (remainMinutes > 0) {
                return days + "天" + remainMinutes + "分钟";
            } else {
                return days + "天";
            }
        } else if (hours >= 1) {
            // 几小时：显示 "3小时30分钟"
            long remainMinutes = minutes % 60;

            if (remainMinutes > 0) {
                return hours + "小时" + remainMinutes + "分钟";
            } else {
                return hours + "小时";
            }
        } else if (minutes >= 1) {
            // 不到一小时：显示 "30分钟"
            return minutes + "分钟";
        } else {
            // 不到一分钟
            return "刚刚";
        }
    }
}
