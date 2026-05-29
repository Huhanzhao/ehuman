package com.bot.service.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bot.service.bean.ChatClientFactory;
import com.bot.service.entity.TbBotLifeDetailDo;
import com.bot.service.entity.TbUserBotStatusDo;
import com.bot.service.enums.MoodEnum;
import com.bot.service.enums.NodeEnum;
import com.bot.service.service.TbBotLifeDetailDoService;
import com.bot.service.service.TbUserBotStatusDoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class BotStatusSearchNode implements NodeAction {

    private final ChatClientFactory chatClientFactory;
    private final TbUserBotStatusDoService tbUserBotStatusDoService;
    private final TbBotLifeDetailDoService tbBotLifeDetailDoService;

    @Override
    public Map<String, Object> apply(OverAllState state) throws InterruptedException {
        log.info("开始心情分析");
        Long userId = state.value("userId", Long.class).orElse(0L);
        String memoryString = state.value("memoryString", String.class).orElse("");

        if (userId == 0) {
            log.error("用户ID为空，跳过");
            return Map.of();
        }

        // 获取当前正在做的事情
        Date now = new Date();
        TbBotLifeDetailDo currentLife = tbBotLifeDetailDoService.getOne(
                Wrappers.<TbBotLifeDetailDo>lambdaQuery()
                        .le(TbBotLifeDetailDo::getStartTime, now)
                        .ge(TbBotLifeDetailDo::getEndTime, now)
                        .last("limit 1")
        );
        String lifeDetail = currentLife != null ? currentLife.getDetail() : "无特定安排";

        // 获取当前心情
        TbUserBotStatusDo statusDo = tbUserBotStatusDoService.getByUserId(userId);
        if (statusDo == null) {
            statusDo = new TbUserBotStatusDo().setUserId(userId).setMood(0).setGrace(0).setFamiliarity(0);
            tbUserBotStatusDoService.save(statusDo);
        }
        MoodEnum currentMood = MoodEnum.fromCode(statusDo.getMood() != null ? statusDo.getMood() : 0);

        String prompt = "你是一个心情分析专家，负责分析情绪陪伴机器人当前应该处于什么心情。\n\n" +
                "## 心情选项\n" +
                "0 - 平静：日常状态，没有明显情绪波动\n" +
                "1 - 开心：遇到好事、收到关心、有趣的话题\n" +
                "2 - 悲伤：受到委屈、想念、感伤的话题\n" +
                "3 - 愤怒：被冒犯、被辱骂、被不尊重\n" +
                "4 - 烦躁：被打扰、不耐烦、压力大\n\n" +
                "## 当前状态\n" +
                "机器人正在做的事情：" + lifeDetail + "\n" +
                "## 最近的对话\n" +
                memoryString + "\n\n" +
                "## 分析要求\n" +
                "1. 结合机器人正在做的事情和对话内容，判断当前最合理的心情\n" +
                "2. 心情变化不要太剧烈，除非对话中有强烈的情绪触发\n\n" +
                "## 输出格式\n" +
                "心情为平静时只输出一行：\n" +
                "心情: 0\n" +
                "心情不平静时输出两行：\n" +
                "心情: 数字\n" +
                "原因: 简短原因（一句话）\n\n" +
                "不要输出其他内容。";

        try {
            ChatClient chatClient = chatClientFactory.createChatClient(userId, NodeEnum.MOOD);
            String content = chatClient.prompt(prompt).call().content();
            log.info("心情分析原始结果: {}", content);

            // 提取心情数字
            int moodCode = currentMood.getCode();
            Matcher moodMatcher = Pattern.compile("心情[:：]\\s*([0-4])").matcher(content);
            if (moodMatcher.find()) {
                moodCode = Integer.parseInt(moodMatcher.group(1));
            } else {
                Matcher fallback = Pattern.compile("[0-4]").matcher(content.trim());
                if (fallback.find()) {
                    moodCode = Integer.parseInt(fallback.group());
                }
            }

            // 提取原因
            String reason = "";
            if (moodCode != MoodEnum.CALM.getCode()) {
                Matcher reasonMatcher = Pattern.compile("原因[:：]\\s*(.+)").matcher(content);
                if (reasonMatcher.find()) {
                    reason = reasonMatcher.group(1).trim();
                }
            }

            MoodEnum newMood = MoodEnum.fromCode(moodCode);
            tbUserBotStatusDoService.updateMoodByUserId(userId, moodCode,reason);
            log.info("用户{}心情更新: {} -> {} {}", userId, currentMood.getDesc(), newMood.getDesc(),
                    reason.isEmpty() ? "" : "原因: " + reason);

            return Map.of("mood", moodCode, "moodReason", reason, "grace", statusDo.getGrace(), "familiarity", statusDo.getFamiliarity());
        } catch (Exception e) {
            log.error("心情分析失败, userId={}", userId, e);
            return Map.of("mood", currentMood.getCode(), "moodReason", "", "grace", statusDo.getGrace(), "familiarity", statusDo.getFamiliarity());
        }
    }
}
