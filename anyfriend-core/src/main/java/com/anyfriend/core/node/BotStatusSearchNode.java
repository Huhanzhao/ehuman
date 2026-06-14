package com.anyfriend.core.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.fastjson2.util.DateUtils;
import com.anyfriend.core.bean.ChatClientFactory;
import com.anyfriend.dao.entity.*;
import com.anyfriend.dao.enums.FamiliarityEnum;
import com.anyfriend.dao.enums.MoodEnum;
import com.anyfriend.dao.enums.NodeEnum;
import com.anyfriend.dao.enums.PromptTypeEnum;
import com.anyfriend.dao.service.*;
import com.anyfriend.plugin.SystemPath;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class BotStatusSearchNode implements AsyncNodeActionWithConfig {

    private final ChatClientFactory chatClientFactory;
    private final TbUserBotStatusDoService tbUserBotStatusDoService;
    private final TbBotLifeDetailDoService tbBotLifeDetailDoService;
    private final TbUserInfoDoService tbUserInfoDoService;
    private final TbUserMessageSummaryDoService tbUserMessageSummaryDoService;
    private final TbBotPromptDoService tbBotPromptDoService;
    private final TbAiChatMemoryService tbAiChatMemoryService;
    private final SystemPath systemPath;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        // todo 把生成消息节点前面的查询逻辑挪到这里
        log.info("开始心情分析");
        Long userId = state.value("userId", Long.class).orElse(0L);
        String memoryString = state.value("memoryString", String.class).orElse("");

        if (userId == 0) {
            log.error("用户ID为空，跳过");
            return CompletableFuture.completedFuture(Map.of());
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
            statusDo = new TbUserBotStatusDo().setUserId(userId).setMoodEnum(MoodEnum.CALM).setGrace(0).setFamiliarity(0);
            tbUserBotStatusDoService.save(statusDo);
        }
        MoodEnum currentMood = statusDo.getMoodEnum();

        String prompt = "你是一个心情分析专家，负责分析情绪陪伴机器人当前应该处于什么心情。\n\n" +
                "## 心情选项\n" +  // todo 用户和你现在的关系
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
            tbUserBotStatusDoService.updateMoodByUserId(userId, newMood,reason);
            log.info("用户{}心情更新: {} -> {} {}", userId, currentMood.getDesc(), newMood.getDesc(),
                    reason.isEmpty() ? "" : "原因: " + reason);

            // 合并心情分析结果和提示词变量，通过返回值传递给下一个节点
            Map<String, Object> result = new HashMap<>(getPromptVariables(state));
            result.put("mood", moodCode);
            result.put("moodReason", reason);
            result.put("grace", statusDo.getGrace());
            result.put("familiarity", statusDo.getFamiliarity());
            result.put("lifeDetail", lifeDetail);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("心情分析失败, userId={}", userId, e);
            Map<String, Object> result = new HashMap<>(getPromptVariables(state));
            result.put("mood", currentMood.getCode());
            result.put("moodReason", "");
            result.put("grace", statusDo.getGrace());
            result.put("familiarity", statusDo.getFamiliarity());
            result.put("lifeDetail", lifeDetail);
            return CompletableFuture.completedFuture(result);
        }
    }

    private Map getPromptVariables(OverAllState state) {
        // 获取bot状态
        Integer reTry = (Integer)state.value("reTry").orElse(0);
        Integer moodCode = (Integer)state.value("mood").orElse(0);
        Integer grace = (Integer)state.value("grace").orElse(0);
        String target = (String) state.value("target").orElse("");
        String moodReason = (String)state.value("moodReason").orElse("");
        Integer familiarityScore = (Integer)state.value("familiarity").orElse(0);
        Long userId = (Long)state.value("userId").orElse(0L);

        // 获取枚举
        MoodEnum moodEnum = MoodEnum.fromCode(moodCode);
        FamiliarityEnum familiarityEnum = FamiliarityEnum.fromScore(familiarityScore);

        // 构建心情提示词
        String moodPrompt = buildMoodPrompt(moodEnum, moodReason);

        // 构建亲密度提示词
        String familiarityPrompt = buildFamiliarityPrompt(familiarityEnum, familiarityScore);

        // 构建好感度提示词
        String gracePrompt = buildGracePrompt(grace);

        List<String> userInfoList = tbUserInfoDoService.getUserInfoByUserId(userId).stream().map(TbUserInfoDo::getUserInfo).toList();

        // 查询bot当前正在做的事
        Date now = new Date();
        String lifeDetail = tbBotLifeDetailDoService.getCurrentActivity(Long.valueOf(System.getProperty("BOT_ID")));

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
            daysInfo = "今天是你们加上好友的第" + days + "天";
        }

        // 查询最后一条用户消息的时间
        String lastUserMessage = tbAiChatMemoryService.getLastUserMessage(userId);

        // 构建时间信息
        String currentTime = "今天是" + DateUtils.format(now, "yyyy年") + LocalDateTime.now().getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.CHINA)
                + "，今天是" + DateUtils.format(now, "MM月dd日 HH:mm")
                + lastUserMessage;

        // 查询昨天的聊天总结
        LocalDateTime yesterdayStart = LocalDateTime.now().minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime yesterdayEnd = LocalDateTime.now().minusDays(1).withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        TbUserMessageSummaryDo yesterdaySummary = tbUserMessageSummaryDoService.getOne(
                Wrappers.<TbUserMessageSummaryDo>lambdaQuery()
                        .eq(TbUserMessageSummaryDo::getUserId, userId)
                        .ge(TbUserMessageSummaryDo::getCreateTime, yesterdayStart)
                        .le(TbUserMessageSummaryDo::getCreateTime, yesterdayEnd)
                        .orderByDesc(TbUserMessageSummaryDo::getCreateTime)
                        .last("limit 1")
        );
        String yesterdayChatSummary = yesterdaySummary != null ? yesterdaySummary.getSummary() : "无";

        String botRole = tbBotPromptDoService.getContext(PromptTypeEnum.BOT_ROLE);

        // 查看当前是第几次生成消息
        if (reTry == 0){
            state.input(Map.of("reTry", 1));
        }

        String stickerTypes = null;

        try {
            stickerTypes = Files.list(Paths.get(systemPath.getEmojiBasePath()))
                    .toList().stream()
                    .map(path -> path.getFileName().toString())
                    .toList().toString();
        } catch (IOException e) {
            stickerTypes = "无";
        }

        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("botRole", botRole);
        resultMap.put("target", target);
        resultMap.put("mood", moodPrompt);
        resultMap.put("familiarity", familiarityPrompt);
        resultMap.put("grace", gracePrompt);
        resultMap.put("currentTime", currentTime);
        resultMap.put("currentActivity", lifeDetail.isBlank() ? "" : "你现在正在" + lifeDetail);
        resultMap.put("userInfo", userInfoList.isEmpty() ? "无" : userInfoList.getFirst());
        resultMap.put("yesterdayChatSummary", yesterdayChatSummary);
        resultMap.put("daysTogether", daysInfo);
        resultMap.put("stickerTypes", stickerTypes);
        if (reTry > 0){
            List<String> failReasons = (List<String>) state.value("failReason").orElse(null);
            resultMap.put("reTryPrompt", "系统信息：这是第" + (reTry + 1) + "次为本轮生成消息\n " +
                    "**前面生成的消息审核失败的原因：**"
                    +  String.join("\n", failReasons)
                    + "\n 请避免这些原因 按照要求重新为本次会话生成消息"
            );
        }else{
            resultMap.put("reTryPrompt", "");
        }
        return resultMap;
    }



    /**
     * 构建心情提示词
     */
    private String buildMoodPrompt(MoodEnum moodEnum, String moodReason) {
        String base = switch (moodEnum) {
            case CALM -> "当前心情平静，语气自然平稳";
            case HAPPY -> "当前心情开心，语气轻快活泼，带点笑意";
            case SAD -> "当前心情低落，语气略带感伤，可能需要安慰";
            case ANGRY -> "当前心情不好，语气冷淡克制，可能带点刺";
            case IRRITABLE -> "当前有点烦躁，语气不耐烦，回复简短";
        };

        if (moodEnum != MoodEnum.CALM && !moodReason.isEmpty()) {
            base += "（原因：" + moodReason + "）";
        }
        return base;
    }

    /**
     * 构建亲密度提示词
     */
    private String buildFamiliarityPrompt(FamiliarityEnum familiarityEnum, int score) {
        String desc = switch (familiarityEnum) {
            case STRANGER -> "你们还不熟，保持礼貌客气，称呼正式，不聊私密话题";
            case NORMAL -> "关系普通，正常交流，可以聊日常，但不过分主动";
            case FAMILIAR -> "比较熟悉了，说话放松自然，可以开玩笑了，偶尔关心对方";
            case CLOSE -> "关系亲密，可以撒娇、主动关心，聊深入话题，称呼亲昵";
            case BELOVED -> "非常喜欢对方，语气黏人，情感表达丰富，会主动撩、表达想念";
        };
        return "亲密度：" + score + "/200（" + familiarityEnum.getDesc() + "）" + desc;
    }

    /**
     * 构建好感度提示词
     */
    private String buildGracePrompt(int grace) {
        String desc;
        if (grace <= -60) {
            desc = "极度反感，可以不回复";
        } else if (grace <= -20) {
            desc = "印象不好，回复冷淡简短";
        } else if (grace <= 20) {
            desc = "印象一般，正常交流，不刻意热情";
        } else if (grace <= 50) {
            desc = "印象不错，语气温和，可以适当关心";
        } else if (grace <= 75) {
            desc = "印象很好，语气偏暖，偶尔流露关心";
        } else if (grace <= 90) {
            desc = "印象非常好，语气温柔，可以适度暧昧和撒娇";
        } else {
            desc = "非常喜欢，语气黏人，主动关心，情感丰富";
        }
        return "好感度：" + grace + "/100，" + desc;
    }
}
