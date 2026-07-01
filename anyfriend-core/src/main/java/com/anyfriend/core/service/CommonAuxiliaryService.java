package com.anyfriend.core.service;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.fastjson2.JSON;
import com.anyfriend.core.bean.ChatClientFactory;
import com.anyfriend.core.job.JobManager;
import com.anyfriend.core.utils.SendMessageUtil;
import com.anyfriend.dao.enums.NodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
public class CommonAuxiliaryService {

    @Autowired
    private JobManager jobManager;

    @Autowired
    private ChatClientFactory chatClientFactory;

    @Autowired
    private SendMessageUtil sendMessageUtil;

    private String formatOutPut = """
            ## 输出格式(nextSend 必须为true，除非当前ai生气，或者用户多次不回复消息才能输出false)
            **用户回复消息之前不主动给对方发消息**：
            {
              "nextSend": false,
              "reason": "xxx"
            }
                        
            **用户回复消息之前主动给对方发消息**：
            {
              "nextSend" : true,
              "nextSendTime": "2026-06-28 00:00:00",
              "topic": "xxx"
            }
                        
            ## 实例
            1. {
                 "nextSend" : true,
                 "nextSendTime": "2026-06-29 18:25:00",
                 "topic": "问对方在干嘛"
               }
            2. {
                  "nextSend" : true,
                  "nextSendTime": "2026-06-30 18:30:00",
                  "topic": "问对方为什么不回我消息"
               }
            3. {
                  "nextSend" : true,
                  "nextSendTime": "2026-06-30 18:30:00",
                  "topic": "分享生活"
                  }
            4. {
                 "nextSend": false,
                 "reason": "不理我我也不理你"
               }
            5. {
               "nextSend": false,
               "reason": "一直骂我，再也不想理你了"
               }
            """;

    //  更新下次主动发送消息的信息
    @Transactional
    public String updateNextSendMessageInfo(Long userId, OverAllState state) {
        ChatClient chatClient = chatClientFactory.createChatClient(userId, NodeEnum.AUXILIARY);
        String memoryString = state.value("memoryString", String.class).get();
        String generateMessage = state.value("generateMessage", String.class).get().trim();
        PromptTemplate promptTemplate = null;
        try {
            promptTemplate = PromptTemplate.builder().template(
                    Files.readString(Paths.get("./md/NextSendMessage.md"))).build();
        } catch (IOException e) {
            log.error("读取文件异常");
            throw new RuntimeException("读取文件异常");
        }
        state.updateState(Map.of("memoryString", memoryString + "assistant : " + generateMessage));
        Map<String, Object> data = state.data();
        String prompt = promptTemplate.render(data) + formatOutPut;
        log.info(prompt);
        String output = chatClient.prompt(prompt).call().content();
        log.info("定时任务更新输出结果:{}", output);

        // json解析
        try {
            Map<String,Object> resultMap = JSON.parseObject(output.replaceAll("\n","").trim());
            jobManager.deleteJobByStartString("nextSendMessage_");
            if ((Boolean) resultMap.get("nextSend")){
                jobManager.addJob("nextSendMessage_" + System.currentTimeMillis(),
                        "system",
                        toCronExpression(resultMap.get("nextSendTime").toString()),
                        "com.anyfriend.core.job.action.SendMessageJob",
                        resultMap.get("topic").toString());
            }else{
                log.info("下次不主动发送消息,原因：{}", resultMap.get("reason"));
                sendMessageUtil.sendMegToSelf(resultMap.get("reason").toString());
            }
        } catch (Exception e) {
            log.error("更新定时任务失败：{}",e.getMessage());
            sendMessageUtil.sendMegToSelf("更新定时任务失败：" + e.getMessage());
            throw new RuntimeException("更新定时任务失败");
        }
        return "";
    }


    /**
     * 将日期时间字符串转换为 Cron 表达式
     *
     * @param dateTimeStr 日期时间字符串，格式：yyyy-MM-dd HH:mm:ss
     * @return Cron 表达式，格式：秒 分 时 日 月 周
     */
    public String toCronExpression(String dateTimeStr) {
        // 1. 解析时间字符串
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, formatter);

        // 2. 提取各个字段
        int second = dateTime.getSecond();
        int minute = dateTime.getMinute();
        int hour = dateTime.getHour();
        int day = dateTime.getDayOfMonth();
        int month = dateTime.getMonthValue();
        // 星期几：Cron 中 1-7 表示 周日-周六，Java 中 1-7 表示 周一-周日
        // 简单处理：只执行指定日期，不限制星期，用 ? 表示忽略
        String week = "?";

        // 3. 组装 Cron 表达式
        return String.format("%d %d %d %d %d %s", second, minute, hour, day, month, week);
    }
}
