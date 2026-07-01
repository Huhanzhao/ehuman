package com.anyfriend.core.utils;

import com.anyfriend.dao.entity.TbAiChatMemoryDo;
import org.springframework.ai.chat.messages.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class MemoryCastUtils {

    public static String memoryCastToString(List<TbAiChatMemoryDo> memoryDoList) {
        List<TbAiChatMemoryDo> tbAiChatMemoryDos = fillSystemMessage(memoryDoList);
        StringBuilder result = new StringBuilder();
        tbAiChatMemoryDos.forEach(memoryDo -> {
            result.append(memoryDo.getRole()).append(" : ").append(memoryDo.getMessage())
                    .append("\n");
        });
        return result.toString();
    }

    public static List<Message> memoryCastToMessages(List<TbAiChatMemoryDo> memoryDoList) {
        List<TbAiChatMemoryDo> tbAiChatMemoryDos = fillSystemMessage(memoryDoList);
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

    public static List<TbAiChatMemoryDo> fillSystemMessage(List<TbAiChatMemoryDo> memoryDoList) {
        // 消息拼接成ai user 交替的， memoryDoList 是降序limit之后反转的
        List<TbAiChatMemoryDo> resultList = new ArrayList<>();

        boolean todayFirstMessage = true;
        for (int i = 0; i < memoryDoList.size(); i++) {
            TbAiChatMemoryDo currentMemoryDo = memoryDoList.get(i);

            boolean isToday = currentMemoryDo.getCreateTime()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .equals(LocalDate.now());
            if (isToday && todayFirstMessage){
                resultList.add(new TbAiChatMemoryDo()
                        .setRole(MessageType.SYSTEM.getValue())
                        .setMessage("========第二天=======")
                        .setCreateTime(new Date())
                );
                todayFirstMessage = false;
            }

            long minutes = 0;
            TbAiChatMemoryDo lastMemoryDo = null;
            if (i != 0){
                // 从最早的消息开始循环，判断与上一条消息的时间间隔
                lastMemoryDo = resultList.getLast();
                minutes = Duration.between(lastMemoryDo.getCreateTime().toInstant(), currentMemoryDo.getCreateTime().toInstant()).toMinutes();
            }

            if (i == 0 || minutes >= 60){
                // 如果间隔超过五分钟 就插入一个系统消息提示时间
                resultList.add(new TbAiChatMemoryDo()
                        .setMessage(formatRelativeTime(currentMemoryDo.getCreateTime()))
                        .setRole(MessageType.SYSTEM.getValue()));
                resultList.add(currentMemoryDo);
            }else{
                // 如果没有超过5分钟
                // 就判断消息类型，一样的消息类型并且间隔小于一分钟就用换行合并在一起
                if (currentMemoryDo.getRole().equals(lastMemoryDo.getRole()) && minutes < 1){
                    resultList.getLast().setMessage(resultList.getLast().getMessage() + "\n" + currentMemoryDo.getMessage());
                }else{
                    resultList.add(currentMemoryDo);
                }
            }
        }
        // 最后一条消息后面跟上系统时间
        resultList.add(new TbAiChatMemoryDo()
                .setMessage("当前时间：" + formatRelativeTime(new Date()) + " 必须确保生成的消息内容符合当前时间")
                .setRole(MessageType.SYSTEM.getValue()));

        return resultList;
    }

    /**
     * 将时间转换为相对当前时间的可读字符串
     * 今天：中午 12:00
     * 昨天：昨天 8:30
     * 前天：前天 14:00
     * 更早：6月10日 14:00
     */
    public static String formatRelativeTime(Date time) {
        if (time == null) return "";

        LocalDateTime dateTime = time.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDate targetDate = dateTime.toLocalDate();

        long daysBetween = Duration.between(targetDate.atStartOfDay(), today.atStartOfDay()).toDays();

        String timeStr = String.format("%d:%02d", dateTime.getHour(), dateTime.getMinute());

        if (daysBetween == 0) {
            // 今天
            return getTimePeriod(dateTime.getHour()) + " " + timeStr;
        } else if (daysBetween == 1) {
            // 昨天
            return "昨天 " + timeStr;
        } else if (daysBetween == 2) {
            // 前天
            return "前天 " + timeStr;
        } else {
            // 更早：几月几日 几点几分
            return dateTime.getMonthValue() + "月" + dateTime.getDayOfMonth() + "日 " + timeStr;
        }
    }

    /**
     * 根据小时返回时间段描述
     */
    private static String getTimePeriod(int hour) {
        if (hour >= 5 && hour < 8) return "清晨";
        if (hour >= 8 && hour < 11) return "上午";
        if (hour >= 11 && hour < 13) return "中午";
        if (hour >= 13 && hour < 17) return "下午";
        if (hour >= 17 && hour < 19) return "傍晚";
        if (hour >= 19 && hour < 22) return "晚上";
        return "深夜";
    }
}
