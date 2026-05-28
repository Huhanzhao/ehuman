package com.bot.service.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson2.util.DateUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bot.service.common.entity.BusinessException;
import com.bot.service.bean.ChatClientFactory;
import com.bot.service.entity.TbAiChatMemoryDo;
import com.bot.service.entity.TbBotLifeDetailDo;
import com.bot.service.entity.TbUserBotStatusDo;
import com.bot.service.entity.TbUserInfoDo;
import com.bot.service.enums.NodeEnum;
import com.bot.service.service.TbAiChatMemoryService;
import com.bot.service.service.TbBotLifeDetailDoService;
import com.bot.service.service.TbUserBotStatusDoService;
import com.bot.service.service.TbUserInfoDoService;
import com.bot.service.utils.MemoryCastUtils;
import com.bot.service.utils.SendMessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@RequiredArgsConstructor
@Slf4j
@Component
public class MessageGenerateNode implements NodeAction {

    private final ChatClientFactory chatClientFactory;
    private final TbAiChatMemoryService tbAiChatMemoryService;
    private final TbUserInfoDoService tbUserInfoDoService;
    private final TbBotLifeDetailDoService tbBotLifeDetailDoService;
    private final TbUserBotStatusDoService tbUserBotStatusDoService;
    private final SendMessageUtil sendMessageUtil;

    @Override
    public Map<String, Object> apply(OverAllState state) {
//        String userMessage = (String)state.value("userInput").orElse("");
        Long userId = (Long)state.value("userId").orElse(0L);
        StopWatch stopwatch = new StopWatch("messageGenerateNode");
        stopwatch.start();
        List<Message> messageList = MemoryCastUtils.memoryCastToMessages(
                tbAiChatMemoryService.getMessageList(userId, 15));
        stopwatch.stop();
        log.info("messageGenerateNode:{}", stopwatch.prettyPrint());
        if (messageList.isEmpty()){
            throw new BusinessException("用户输入读取失败");
        }
        ChatClient chatClient = chatClientFactory.createChatClient(userId, NodeEnum.GENERATE);

        String template = null;
        try {
            template = Files.readString(Paths.get("ai-bot-service/md/MainPrompt.md"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        PromptTemplate promptTemplate = PromptTemplate.builder().template(template).build();

        TbUserBotStatusDo graceDo = tbUserBotStatusDoService.getByUserId(userId);
        if (graceDo == null) {
            graceDo = new TbUserBotStatusDo().setUserId((long)userId).setGrace(0);
            tbUserBotStatusDoService.save(graceDo);
        }
        Integer grace = graceDo.getGrace();

        String gracePrompt =  "当前好感度：" + grace + "(-100~100) " ;
        // 区间控制
        if (grace <= -60){
            gracePrompt += "当前好感度极低，语气明显冷淡疏远，回复非常简短（1行），不表达情绪，不主动延续话题，保持距离感。不回复概率很高";
        }
        else if (grace <= -20){
            gracePrompt += "当前好感度偏低，语气克制疏离，回复较短（1-2行），减少情绪表达，不主动延续话题。不回复概率较高";
        }
        else if (grace <= 20){
            gracePrompt += "当前好感度一般，保持自然普通交流，语气平稳，回复2行左右，不刻意热情，也不过分冷淡。不回复概率一般";
        }
        else if (grace <= 50){
            gracePrompt += "当前好感度较好，语气温和自然，回复2-3行，可以轻微表达关心，但不过度主动。不回复概率很低";
        }
        else if (grace <= 75){
            gracePrompt += "当前好感度较高，语气柔和偏暖，回复2-3行，主动程度略高，偶尔流露关心或轻微暧昧，但保持自然克制。不回复概率很低";
        }
        else if (grace <= 90){
            gracePrompt += "当前好感度很高，语气温柔，情绪表达明显增加，回复3行左右，接受对方撩骚，可以适度暧昧和轻微撒娇，但不要直接表白。不回复概率很低";
        }
        else {
            gracePrompt += "当前好感度极高，语气温柔黏人，明显在意对方，主动关心，主动撩骚，欲望旺盛，可轻微表达依赖感，但保持真实自然不过火。不回复概率很低";
        }

        List<String> userInfoList = tbUserInfoDoService.getUserInfoByUserId(userId).stream().map(TbUserInfoDo::getUserInfo).toList();

        // 查询bot当前正在做的事
        Date now = new Date();
        TbBotLifeDetailDo currentLife = tbBotLifeDetailDoService.getOne(
                Wrappers.<TbBotLifeDetailDo>lambdaQuery()
                        .le(TbBotLifeDetailDo::getStartTime, now)
                        .ge(TbBotLifeDetailDo::getEndTime, now)
                        .last("limit 1")
        );
        String lifeDetail = currentLife != null ? currentLife.getDetail() : "";
        // todo 当时之前的事情也加上 增加心情节点 抽出公用消息生成代码

        // 查询加上好友的天数（从第一条消息开始算）
        TbAiChatMemoryDo firstMessage = tbAiChatMemoryService.getOne(
                Wrappers.<TbAiChatMemoryDo>lambdaQuery()
                        .eq(TbAiChatMemoryDo::getMemoryId, userId)
                        .orderByAsc(TbAiChatMemoryDo::getCreateTime)
                        .last("limit 1")
        );
        String daysInfo = "";
        if (firstMessage != null && firstMessage.getCreateTime() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(
                    firstMessage.getCreateTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                    java.time.LocalDate.now()
            );
            daysInfo = "。今天是你们加上好友的第" + days + "天";
        }

        Prompt prompt = promptTemplate.create(Map.of(
                "grace", gracePrompt,
                "scene", "当前时间：" +
                        DateUtils.format(now,"yyyy-MM-dd HH:mm:ss") +  " " + LocalDateTime.now().getDayOfWeek()
                        + daysInfo
                        + (lifeDetail.isBlank() ? "" : "。你现在正在" + lifeDetail)
                ,"userInfo", userInfoList.isEmpty()? "无" : userInfoList.getFirst()
        ));

        String content = chatClient.prompt(prompt)
                    .messages(messageList).call().content();
//        List<Message> historyList = new ArrayList<>(messageList);
//        historyList.add(new AssistantMessage(content));
        log.info("结果输出：{}" , content);
        sendMessageUtil.sendChatMsg(userId,content);
        return Map.of();
    }
}
