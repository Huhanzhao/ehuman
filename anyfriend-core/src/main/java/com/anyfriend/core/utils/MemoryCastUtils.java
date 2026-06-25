package com.anyfriend.core.utils;

import com.anyfriend.dao.entity.TbAiChatMemoryDo;
import org.springframework.ai.chat.messages.*;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class MemoryCastUtils {

    public static String memoryCastToString(List<TbAiChatMemoryDo> memoryDoList) {
        List<TbAiChatMemoryDo> tbAiChatMemoryDos = fillSystemTimeMessage(memoryDoList);
        StringBuilder result = new StringBuilder();
        tbAiChatMemoryDos.forEach(memoryDo -> {
            result.append(memoryDo.getRole()).append(" : ").append(memoryDo.getMessage())
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
            // 出现ai消息大于五分钟放入旁白 todo 修改逻辑，现在是什么时间
            if (MessageType.ASSISTANT.getValue().equals(memoryDoList.get(i).getRole())){

                Date lastUserMessageTime = memoryDoList.get(i).getCreateTime();
                if (lastUserMessageTime.getTime() < System.currentTimeMillis() - 5 * 60 * 1000){
                    String timeAgo = getTimeAgo(lastUserMessageTime);
                    SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日 HH:mm");
                    String formatted = sdf.format(new Date());
                    // 插入到这条 AI 消息的后面
                    TbAiChatMemoryDo tbAiChatMemoryDo = new TbAiChatMemoryDo()
                            .setRole(MessageType.SYSTEM.getValue())
                            .setMessage("[旁白：过了 " + timeAgo + "之后 现在时间:" + formatted + ", 注意话题延续的时间合理性，不用强行延续]");
                    memoryDoList.add(i + 1, tbAiChatMemoryDo);
                }
                break;
            }
        }
        return memoryDoList;
    }

    public static List<TbAiChatMemoryDo> fillSystemMessage(List<TbAiChatMemoryDo> memoryDoList) {
        // todo 消息拼接成ai user 交替的， memoryDoList 是降序limit之后反转的
        List<TbAiChatMemoryDo> result = new ArrayList<>();
        for (int i = 0; i < memoryDoList.size(); i++) {
            TbAiChatMemoryDo currentMemoryDo = memoryDoList.get(i);
            // 先给一个最早的系统时间消息
            if (i == 0){
                result.add(new TbAiChatMemoryDo()
                        .setMessage(formatRelativeTime(currentMemoryDo.getCreateTime()))
                        .setRole(MessageType.SYSTEM.getValue()));
                result.add(currentMemoryDo);
                continue;
            }

            // 从最早的消息开始循环，判断与上一条消息的时间间隔
                // 如果间隔超过五分钟 就插入一个系统消息提示时间
                // 如果没有超过 就判断消息类型，一样的消息类型并且间隔小于一分钟就用换行合并在一起
        }
        // 最后一条消息后面跟上系统时间
        result.add(new TbAiChatMemoryDo()
                .setMessage("当前时间：" + formatRelativeTime(new Date()))
                .setRole(MessageType.SYSTEM.getValue()));

        return result;
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
