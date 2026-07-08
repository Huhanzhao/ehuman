package com.anyfriend.core.service;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.fastjson2.JSON;
import com.anyfriend.core.bean.ChatClientFactory;
import com.anyfriend.core.job.JobManager;
import com.anyfriend.core.node.LlmNode;
import com.anyfriend.core.utils.LocalCacheUtil;
import com.anyfriend.core.utils.MemoryCastUtils;
import com.anyfriend.core.utils.SendMessageUtil;
import com.anyfriend.dao.entity.TbAiChatMemoryDo;
import com.anyfriend.dao.entity.TbMessageSummaryVectorDo;
import com.anyfriend.dao.entity.TbUserModelConfigDo;
import com.anyfriend.dao.enums.LlmNodeTypeEnum;
import com.anyfriend.dao.enums.NodeEnum;
import com.anyfriend.dao.service.DBVectorOperationService;
import com.anyfriend.dao.service.TbAiChatMemoryService;
import com.anyfriend.dao.service.TbMessageSummaryVectorDoService;
import com.anyfriend.dao.service.TbUserModelConfigDoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
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

    //  更新下次主动发送消息的信息
    public String updateNextSendMessageInfo(Long userId, OverAllState state) {
        LlmNode llmNode = new LlmNode(LlmNodeTypeEnum.UPDATE_NEXT_SEND_MESSAGE_INFO, NodeEnum.AUXILIARY, chatClientFactory);

        Map<String, Object> promptParam = state.data();
        String memoryString = state.value("memoryString", String.class).get();
        String generateMessage = state.value("generateMessage", String.class).get().trim();
        promptParam.put("memoryString", memoryString + "assistant : " + generateMessage);

        String output = llmNode.invoke(userId,promptParam);
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

    public boolean summaryMessages(Long userId){
        if(true) return true; // todo 别忘记去掉
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
                Map<String,Object> promptParams = new HashMap<>();
                // 取出当前批次的20条
                List<TbAiChatMemoryDo> batch = messageList.subList(processedCount, processedCount + 20);
                String memoryString = MemoryCastUtils.memoryCastToString(batch,false);
                promptParams.put("memoryString", memoryString);

                // 查询最近的一次流水账
                TbMessageSummaryVectorDo lastDo = tbMessageSummaryVectorDoService.getLastOneByUserId();
                promptParams.put("lastMemoryString",lastDo == null? "":lastDo.getContext());

                // 调用大模型总结
                LlmNode llmNode = new LlmNode(LlmNodeTypeEnum.SUMMARY_MESSAGES, NodeEnum.AUXILIARY, chatClientFactory);
                String content = llmNode.invoke(userId, promptParams);

                // 保存向量
                embeddingContextAndSave(userId,content,batch.getFirst().getCreateTime());

                // 标记这20条已处理
                processedCount += 20;
                // 标记消息已经向量化
                List<Long> idList = batch.stream().map(TbAiChatMemoryDo::getId).toList();
                tbAiChatMemoryService.setVectorFlagByIds(idList);
            }
            return true;
        }finally {
            LocalCacheUtil.getLocalCacheUtil().remove(userId + "_SummaryMessages");
        }
    }

    public void embeddingContextAndSave(Long userId, String content, Date messageTime){
        // 查询向量化的key
        TbUserModelConfigDo modelConfigDo = tbUserModelConfigDoService.getByUserIdAndNodeNum(userId, NodeEnum.EMBEDDING);
        if (modelConfigDo == null){
            throw new RuntimeException("没有向量模型,userId:" + userId);
        }

        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(modelConfigDo.getApiKey())
                .baseUrl(modelConfigDo.getBaseUrl()).build();

        OpenAiEmbeddingOptions openAiEmbeddingOptions = OpenAiEmbeddingOptions.builder()
                .model(modelConfigDo.getModel()).dimensions(1024)
                .build();
        OpenAiEmbeddingModel openAiEmbeddingModel = new OpenAiEmbeddingModel(openAiApi, MetadataMode.NONE,openAiEmbeddingOptions);

        EmbeddingRequest embeddingRequest = new EmbeddingRequest(List.of(content),openAiEmbeddingOptions);
        EmbeddingResponse response = openAiEmbeddingModel.call(embeddingRequest);
        float[] output = response.getResult().getOutput();

        dbVectorOperationService.saveVector(userId, Arrays.toString(output),content,messageTime);
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
