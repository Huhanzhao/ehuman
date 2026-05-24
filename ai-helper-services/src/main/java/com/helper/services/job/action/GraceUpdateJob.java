package com.helper.services.job.action;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.helper.services.bean.ChatClientFactory;
import com.helper.services.entity.TbAiChatMemoryDo;
import com.helper.services.entity.TbUpdateGraceHistoryDo;
import com.helper.services.entity.TbUserGraceDo;
import com.helper.services.enums.NodeEnum;
import com.helper.services.service.TbAiChatMemoryService;
import com.helper.services.service.TbUpdateGraceHistoryDoService;
import com.helper.services.service.TbUserGraceDoService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class GraceUpdateJob implements Job {

    @Autowired
    private TbUserGraceDoService tbUserGraceDoService;

    @Autowired
    private TbUpdateGraceHistoryDoService tbUpdateGraceHistoryDoService;

    @Autowired
    private TbAiChatMemoryService tbAiChatMemoryService;

    @Autowired
    private ChatClientFactory chatClientFactory;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        // 查询所有有好感度记录的用户
        List<TbUserGraceDo> graceList = tbUserGraceDoService.list();
        if (graceList == null || graceList.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        Date todayStart = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date todayEnd = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        for (TbUserGraceDo graceDo : graceList) {
            Long userId = graceDo.getUserId();
            Integer currentGrace = graceDo.getGrace();

            // 查询该用户今天的所有消息
            List<TbAiChatMemoryDo> todayMessages = tbAiChatMemoryService.list(
                    Wrappers.<TbAiChatMemoryDo>lambdaQuery()
                            .eq(TbAiChatMemoryDo::getMemoryId, userId)
                            .ge(TbAiChatMemoryDo::getCreateTime, todayStart)
                            .lt(TbAiChatMemoryDo::getCreateTime, todayEnd)
                            .orderByAsc(TbAiChatMemoryDo::getCreateTime)
            );

            int delta;
            String reason;

            if (todayMessages == null || todayMessages.isEmpty()) {
                // 没有消息，好感度逐渐趋于0
                if (currentGrace > 0) {
                    delta = -1;
                    reason = "今日无互动，好感度-1";
                } else if (currentGrace < 0) {
                    delta = 1;
                    reason = "今日无互动，好感度+1";
                } else {
                    continue;
                }
            } else {
                // 有消息，用AI评分
                AiEvaluation evaluation = evaluateByAi(userId, todayMessages);
                delta = evaluation.delta();
                reason = evaluation.reason();
            }

            // 更新好感度，限制在-100~100范围内
            int newGrace = Math.max(-100, Math.min(100, currentGrace + delta));
            tbUserGraceDoService.updateGraceByUserId(userId, newGrace);

            // 记录好感度修改历史
            TbUpdateGraceHistoryDo history = new TbUpdateGraceHistoryDo();
            history.setUserId(userId);
            if (delta >= 0) {
                history.setIncrease(delta);
                history.setDecrease(0);
            } else {
                history.setIncrease(0);
                history.setDecrease(Math.abs(delta));
            }
            history.setReason(reason);
            tbUpdateGraceHistoryDoService.save(history);

            log.info("用户{}好感度更新: {} -> {} (delta={})", userId, currentGrace, newGrace, delta);
        }
    }

    private AiEvaluation evaluateByAi(Long userId, List<TbAiChatMemoryDo> messages) {
        // 拼接聊天记录
        StringBuilder chatLog = new StringBuilder();
        for (TbAiChatMemoryDo msg : messages) {
            chatLog.append(msg.getRole()).append(": ").append(msg.getMessage()).append("\n");
        }

        String prompt = "你是一个好感度评估专家。请根据用户今天的聊天记录，为用户的好感度变化评分。" +
                "评分范围：-5到+5的整数，正数表示好感度增加，负数表示好感度减少。" +
                "评估维度：聊天频率、态度友好程度、话题积极性、互动质量。" +
                "今天用户的聊天记录如下：\n" + chatLog +
                "\n请严格按以下格式输出，不要输出其他内容：\n评分: <数字>\n原因: <一句话说明>";

        try {
            ChatClient chatClient = chatClientFactory.createChatClient(userId, NodeEnum.WANT_TO);
            String content = chatClient.prompt(prompt).call().content();
            return parseEvaluation(content);
        } catch (Exception e) {
            log.error("AI评估好感度失败，userId={}", userId, e);
            return new AiEvaluation(0, "AI评估失败，默认不调整");
        }
    }

    private AiEvaluation parseEvaluation(String content) {
        if (content == null || content.isBlank()) {
            return new AiEvaluation(0, "AI返回为空");
        }

        // 提取评分
        int delta = 0;
        Pattern scorePattern = Pattern.compile("评分[:：]\\s*(-?\\d+)");
        Matcher scoreMatcher = scorePattern.matcher(content);
        if (scoreMatcher.find()) {
            delta = Math.max(-5, Math.min(5, Integer.parseInt(scoreMatcher.group(1))));
        }

        // 提取原因
        String reason = "AI评估今日聊天";
        Pattern reasonPattern = Pattern.compile("原因[:：]\\s*(.+)");
        Matcher reasonMatcher = reasonPattern.matcher(content);
        if (reasonMatcher.find()) {
            reason = reasonMatcher.group(1).trim();
        }

        return new AiEvaluation(delta, reason);
    }

    private record AiEvaluation(int delta, String reason) {}
}
