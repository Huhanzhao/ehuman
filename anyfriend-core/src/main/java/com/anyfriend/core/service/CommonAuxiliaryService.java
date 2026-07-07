package com.anyfriend.core.service;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.fastjson2.JSON;
import com.anyfriend.core.bean.ChatClientFactory;
import com.anyfriend.core.job.JobManager;
import com.anyfriend.core.utils.MemoryCastUtils;
import com.anyfriend.core.utils.SendMessageUtil;
import com.anyfriend.dao.entity.TbAiChatMemoryDo;
import com.anyfriend.dao.entity.TbMessageSummaryVectorDo;
import com.anyfriend.dao.enums.NodeEnum;
import com.anyfriend.dao.service.TbAiChatMemoryService;
import com.anyfriend.dao.service.TbMessageSummaryVectorDoService;
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
import java.util.HashMap;
import java.util.List;
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

    @Autowired
    private TbMessageSummaryVectorDoService tbMessageSummaryVectorDoService;

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
    @Autowired
    private TbAiChatMemoryService tbAiChatMemoryService;

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

    // todo 流水账总结 把上一段的流水账拼接上？？
    public boolean summaryMessages(Long userId){
        // todo 缓存标记 防止重复处理
        if (true)return false;

        // 直接取所有没有总结过的消息拼接提示词 //创建时间升序排列
        List<TbAiChatMemoryDo> messageList = tbAiChatMemoryService.getAllNoVectorMessageList(userId);
        int messageListSize = messageList.size();

        // 如果消息总数都不超过20条，直接返回
        if (messageListSize <= 20) {
            return false;
        }

        // 循环处理，每次取20条
        int processedCount = 0;
        while (processedCount < messageListSize) {
            // 计算剩余消息数量
            int remaining = messageListSize - processedCount;

            // 如果剩余不足20条，结束循环
            if (remaining <= 20) {
                break;
            }
            Map<String,String> promptParams = new HashMap<>();
            // 取出当前批次的20条
            List<TbAiChatMemoryDo> batch = messageList.subList(processedCount, processedCount + 20);
            String memoryString = MemoryCastUtils.memoryCastToString(batch,false);
            promptParams.put("memoryString", memoryString);

            // 查询最近的一次流水账
            TbMessageSummaryVectorDo lastDo = tbMessageSummaryVectorDoService.getLastOneByUserId();
            promptParams.put("lastMemoryString",lastDo == null? "":lastDo.getContext());
            // todo 调用大模型总结 要注意信息密度，密度低的对话区分出来  todo 把单节点调用大模型的方法抽取出来？
            // todo 保存向量

            // 标记这20条已处理
            processedCount += 20;
        }
        // todo 标记消息已经向量化
        return true;
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
