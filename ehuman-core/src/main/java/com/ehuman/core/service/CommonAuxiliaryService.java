package com.ehuman.core.service;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.fastjson2.JSON;
import com.ehuman.core.bean.ChatClientFactory;
import com.ehuman.core.job.JobManager;
import com.ehuman.core.node.LlmNode;
import com.ehuman.core.utils.LocalCacheUtil;
import com.ehuman.core.utils.MemoryCastUtils;
import com.ehuman.core.utils.SendMessageUtil;
import com.ehuman.dao.entity.TbAiChatMemoryDo;
import com.ehuman.dao.entity.TbMessageSummaryVectorDo;
import com.ehuman.dao.entity.TbUserModelConfigDo;
import com.ehuman.dao.enums.LlmChatEnum;
import com.ehuman.dao.enums.LlmTypeEnum;
import com.ehuman.dao.service.DBVectorOperationService;
import com.ehuman.dao.service.TbAiChatMemoryService;
import com.ehuman.dao.service.TbMessageSummaryVectorDoService;
import com.ehuman.dao.service.TbUserModelConfigDoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    @Autowired
    private TbAiChatMemoryService tbAiChatMemoryService;

    @Autowired
    private DBVectorOperationService dbVectorOperationService;

    @Autowired
    private TbUserModelConfigDoService tbUserModelConfigDoService;

    @Autowired
    private EmbeddingService embeddingService;

    //  更新下次主动发送消息的信息
    public String updateNextSendMessageInfo(Long userId, OverAllState state) {
        LlmNode llmNode = new LlmNode(LlmChatEnum.UPDATE_NEXT_SEND_MESSAGE_INFO, LlmTypeEnum.AUXILIARY, chatClientFactory);

        String memoryString = state.value("memoryString", String.class).get();
        String generateMessage = state.value("generateMessage", String.class).get().trim();
        state.updateState(Map.of("memoryString", memoryString + "assistant : " + generateMessage));

        String output = llmNode.invoke(userId,state.data());
        log.info("定时任务更新输出结果:{}", output);

        // json解析
        try {
            Map<String,Object> resultMap = JSON.parseObject(output.replaceAll("\n","")
                    .replaceAll("```","")
                    .replaceAll("```json","")
                    .trim());
            jobManager.deleteJobByStartString("nextSendMessage_");
            if ((Boolean) resultMap.get("nextSend")){
                jobManager.addJob("nextSendMessage_" + System.currentTimeMillis(),
                        "system",
                        toCronExpression(resultMap.get("nextSendTime").toString()),
                        "com.ehuman.core.job.action.SendMessageJob",
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

    public boolean summaryMessages(Long userId, Integer firstBatchMinMassagesCount){
        // 查询向量化的key
        TbUserModelConfigDo modelConfigDo = tbUserModelConfigDoService.getByUserIdAndNodeNum(userId, LlmTypeEnum.EMBEDDING);
        if (modelConfigDo == null){
            log.info("没有向量模型跳过");
            return false;
        }
        // 缓存标记 防止重复处理
        Object summaryWorkFlag = LocalCacheUtil.getLocalCacheUtil().get(userId + "_SummaryMessages");
        if (summaryWorkFlag != null){
            return false;
        }
        LocalCacheUtil.getLocalCacheUtil().put(userId + "_SummaryMessages",true);

        try{
            // 直接取所有没有总结过的消息拼接提示词 //创建时间升序排列
            List<TbAiChatMemoryDo> messageList = tbAiChatMemoryService.getAllNoVectorMessageList(userId);
            int messageListSize = messageList.size();

            // 如果消息总数都不超过20条，直接返回
            if (messageListSize < firstBatchMinMassagesCount) {
                log.info("消息数量不足{}条，跳过总结",firstBatchMinMassagesCount);
                return false;
            }

            // 循环处理，每次取20条
            int processedCount = 0;
            while (processedCount < messageListSize) {
                // 计算剩余消息数量
                int remaining = messageListSize - processedCount;

                // 如果剩余不足20条，结束循环
                if (remaining < firstBatchMinMassagesCount) {
                    log.info("剩余消息数量不足{}条，结束循环",firstBatchMinMassagesCount);
                    break;
                }
                Map<String,Object> promptParams = new HashMap<>();
                // 取出当前批次的20条
                List<TbAiChatMemoryDo> batch = messageList.subList(processedCount, Math.min(processedCount + 20, messageListSize));
                Integer currentMessagesCount = batch.size();
                String memoryString = MemoryCastUtils.memoryCastToString(batch,false);
                promptParams.put("memoryString", memoryString);

                // 查询最近的一次流水账
                TbMessageSummaryVectorDo lastDo = tbMessageSummaryVectorDoService.getLastOneByUserId(userId);
                promptParams.put("lastMemoryString",lastDo == null? "":lastDo.getContext());

                // 调用大模型总结
                log.info("开始向量化消息");
                LlmNode llmNode = new LlmNode(LlmChatEnum.SUMMARY_MESSAGES, LlmTypeEnum.AUXILIARY, chatClientFactory);
                String content;
                try {
                    content = llmNode.invoke(userId, promptParams);
                }catch (Exception e){
                    log.error("向量化消息失败：{}",e.getMessage());
                    sendMessageUtil.sendMegToSelf("向量化消息失败");
                    throw new RuntimeException("向量化消息失败");
                }

                // 保存向量
                float[] floats = embeddingService.embeddingText(userId, content);
                dbVectorOperationService.saveVector(userId, Arrays.toString(floats),content,batch.getFirst().getCreateTime());

                // 标记这20条已处理
                processedCount += currentMessagesCount;
                // 标记消息已经向量化
                List<Long> idList = batch.stream().map(TbAiChatMemoryDo::getId).toList();
                tbAiChatMemoryService.setVectorFlagByIds(idList);
                log.info("消息向量化结束，剩余消息{}条",remaining - currentMessagesCount);
            }
            return true;
        }finally {
            LocalCacheUtil.getLocalCacheUtil().remove(userId + "_SummaryMessages");
        }
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
