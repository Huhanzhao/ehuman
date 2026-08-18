package com.anyfriend.core.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.anyfriend.core.bean.ChatClientFactory;
import com.anyfriend.dao.entity.*;
import com.anyfriend.dao.enums.FamiliarityEnum;
import com.anyfriend.dao.enums.LlmChatEnum;
import com.anyfriend.dao.enums.LlmTypeEnum;
import com.anyfriend.dao.enums.MoodEnum;
import com.anyfriend.dao.service.*;
import com.anyfriend.plugin.SystemPath;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.anyfriend.dao.enums.FeatureTypeEnum.VOICE_PLUGIN;

@Component
@RequiredArgsConstructor
@Slf4j
public class BotStatusSearchNode implements AsyncNodeActionWithConfig {

    private static final Pattern MOOD_PATTERN = Pattern.compile("心情[:：]\\s*([0-4])");
    private static final Pattern MOOD_FALLBACK_PATTERN = Pattern.compile("[0-4]");
    private static final Pattern REASON_PATTERN = Pattern.compile("原因[:：]\\s*(.+)");

    private final ChatClientFactory chatClientFactory;
    private final TbUserBotStatusDoService tbUserBotStatusDoService;
    private final TbBotLifeDetailDoService tbBotLifeDetailDoService;
    private final TbUserInfoDoService tbUserInfoDoService;
    private final TbUserMessageSummaryDoService tbUserMessageSummaryDoService;
    private final TbBotPromptDoService tbBotPromptDoService;
    private final TbAiChatMemoryService tbAiChatMemoryService;
    private final TbUserFeatureButtonDoService tbUserFeatureButtonDoService;
    private final SystemPath systemPath;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        log.info("开始心情分析");
        Long userId = state.value("userId", Long.class).orElse(0L);
        String memoryString = state.value("memoryString", String.class).orElse("");

        if (userId == 0) {
            log.error("用户ID为空，跳过");
            return CompletableFuture.completedFuture(Map.of());
        }

        String lifeDetail = getCurrentLifeDetail();
        TbUserBotStatusDo statusDo = getOrCreateStatus(userId);
        MoodEnum currentMood = statusDo.getMoodEnum();

        // 分析心情
        MoodAnalysisResult moodResult = analyzeMood(userId,currentMood, lifeDetail, memoryString);
        tbUserBotStatusDoService.updateMoodByUserId(userId, moodResult.mood(), moodResult.reason());
        log.info("用户{}心情更新: {} -> {} {}", userId, currentMood.getDesc(), moodResult.mood().getDesc(),
                moodResult.reason().isEmpty() ? "" : "原因: " + moodResult.reason());

        // 构建提示词变量
        Map<String, Object> result = buildPromptVariables(state, statusDo, lifeDetail);
        // 用最新的心情覆盖（心情分析可能改变了mood）
        result.put("mood", buildMoodPrompt(moodResult.mood(), moodResult.reason()));
        result.put("moodReason", moodResult.reason());
        return CompletableFuture.completedFuture(result);
    }

    // ==================== 心情分析 ====================

    private MoodAnalysisResult analyzeMood(Long userId,MoodEnum currentMood, String lifeDetail, String memoryString) {
        try {
            String content = new LlmNode(LlmChatEnum.MOOD_ANALYSIS, LlmTypeEnum.AUXILIARY, chatClientFactory)
                    .invoke(userId, Map.of("memoryString",memoryString,"lifeDetail",lifeDetail));
            log.info("心情分析原始结果: {}", content);

            int moodCode = extractMoodCode(content, currentMood.getCode());
            String reason = extractMoodReason(content, moodCode);
            MoodEnum newMood = MoodEnum.fromCode(moodCode);
            return new MoodAnalysisResult(newMood, reason);
        } catch (Exception e) {
            log.error("心情分析失败", e);
            return new MoodAnalysisResult(currentMood, "");
        }
    }

    private int extractMoodCode(String content, int defaultCode) {
        Matcher matcher = MOOD_PATTERN.matcher(content);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        Matcher fallback = MOOD_FALLBACK_PATTERN.matcher(content.trim());
        if (fallback.find()) {
            return Integer.parseInt(fallback.group());
        }
        return defaultCode;
    }

    private String extractMoodReason(String content, int moodCode) {
        if (moodCode == MoodEnum.CALM.getCode()) {
            return "";
        }
        Matcher matcher = REASON_PATTERN.matcher(content);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private record MoodAnalysisResult(MoodEnum mood, String reason) {}

    // ==================== 提示词变量构建 ====================

    private Map<String, Object> buildPromptVariables(OverAllState state, TbUserBotStatusDo statusDo, String lifeDetail) {
        Long userId = state.value("userId", Long.class).orElse(0L);
        String target = state.value("target", String.class).orElse("");

        Map<String, Object> result = new HashMap<>();
        result.put("botRole", tbBotPromptDoService.getRolePrompt());
        result.put("target", target);
        result.put("mood", buildMoodPrompt(statusDo.getMoodEnum(), statusDo.getMoodReason()));
        result.put("familiarity", buildFamiliarityPrompt(statusDo.getFamiliarity()));
        result.put("grace", buildGracePrompt(statusDo.getGrace()));
        result.put("voiceMessage", getVoiceMessagePrompt(userId));
        result.put("lifeDetail", lifeDetail);
        result.put("currentTime", buildTimeInfo());
        result.put("currentActivity", lifeDetail.isBlank() ? "" : "你现在正在" + lifeDetail);
        result.put("userInfo", getUserInfo(userId));
        result.put("yesterdayChatSummary", getYesterdayChatSummary(userId));
        result.put("daysTogether", getDaysTogetherInfo(userId));
        result.put("stickerTypes", getStickerTypes());
        result.put("failReason", "");
        return result;
    }

    private String getVoiceMessagePrompt(Long userId) {
        boolean exists = tbUserFeatureButtonDoService.isEnableByUserId(userId, VOICE_PLUGIN);
        return exists ? "| `<voice tone=\"情绪\">内容</voice>` | 语音消息，情绪如撒娇、生气等|" : "只支持以上消息类型";
    }

    // ==================== 数据查询 ====================

    private String getCurrentLifeDetail() {
        Date now = new Date();
        TbBotLifeDetailDo currentLife = tbBotLifeDetailDoService.getOne(
                Wrappers.<TbBotLifeDetailDo>lambdaQuery()
                        .le(TbBotLifeDetailDo::getStartTime, now)
                        .ge(TbBotLifeDetailDo::getEndTime, now)
                        .last("limit 1")
        );
        return currentLife != null ? currentLife.getDetail() : "无特定安排";
    }

    private TbUserBotStatusDo getOrCreateStatus(Long userId) {
        TbUserBotStatusDo statusDo = tbUserBotStatusDoService.getByUserId(userId);
        if (statusDo == null) {
            statusDo = new TbUserBotStatusDo().setUserId(userId).setMoodEnum(MoodEnum.CALM).setGrace(0).setFamiliarity(0);
            tbUserBotStatusDoService.save(statusDo);
        }
        return statusDo;
    }

    private String getUserInfo(Long userId) {
        List<String> infoList = tbUserInfoDoService.getUserInfoByUserId(userId).stream()
                .map(TbUserInfoDo::getUserInfo).toList();
        return infoList.isEmpty() ? "无" : infoList.getFirst();
    }

    private String getYesterdayChatSummary(Long userId) {
        LocalDateTime yesterdayStart = LocalDateTime.now().minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime yesterdayEnd = LocalDateTime.now().minusDays(1).withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        // todo 修改逻辑 昨天的总结不要了，查询最近两段或者一段总结
        TbUserMessageSummaryDo summary = tbUserMessageSummaryDoService.getOne(
                Wrappers.<TbUserMessageSummaryDo>lambdaQuery()
                        .eq(TbUserMessageSummaryDo::getUserId, userId)
                        .ge(TbUserMessageSummaryDo::getCreateTime, yesterdayStart)
                        .le(TbUserMessageSummaryDo::getCreateTime, yesterdayEnd)
                        .orderByDesc(TbUserMessageSummaryDo::getCreateTime)
                        .last("limit 1")
        );
        return summary != null ? summary.getSummary() : "无";
    }

    private String getDaysTogetherInfo(Long userId) {
        TbAiChatMemoryDo firstMessage = tbAiChatMemoryService.getOne(
                Wrappers.<TbAiChatMemoryDo>lambdaQuery()
                        .eq(TbAiChatMemoryDo::getMemoryId, userId)
                        .orderByAsc(TbAiChatMemoryDo::getCreateTime)
                        .last("limit 1")
        );
        if (firstMessage == null || firstMessage.getCreateTime() == null) {
            return "";
        }
        long days = ChronoUnit.DAYS.between(
                firstMessage.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                LocalDate.now()
        );
        return "今天是你们加上好友的第" + days + "天";
    }

    private String buildTimeInfo() {
        LocalDateTime now = LocalDateTime.now();
        String dayOfWeek = now.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.CHINA);
        return "今天是" + now.getYear() + "年" + dayOfWeek + "，今天是"
                + String.format("%02d月%02d日 %02d:%02d", now.getMonthValue(), now.getDayOfMonth(), now.getHour(), now.getMinute());
    }

    private String getStickerTypes() {
        try {
            return Files.list(Paths.get(systemPath.getEmojiBasePath()))
                    .map(path -> path.getFileName().toString())
                    .toList().toString();
        } catch (IOException e) {
            return "无";
        }
    }

    // ==================== 提示词构建 ====================

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

    private String buildFamiliarityPrompt(int score) {
        FamiliarityEnum familiarityEnum = FamiliarityEnum.fromScore(score);
        String desc = switch (familiarityEnum) {
            case STRANGER -> "你们还不熟，保持礼貌客气，称呼正式，不聊私密话题";
            case NORMAL -> "关系普通，正常交流，可以聊日常，但不过分主动";
            case FAMILIAR -> "比较熟悉了，说话放松自然，可以开玩笑了，偶尔关心对方";
            case CLOSE -> "关系亲密，可以撒娇、主动关心，聊深入话题，称呼亲昵";
            case BELOVED -> "非常喜欢对方，语气黏人，情感表达丰富，会主动撩、表达想念";
        };
        return "亲密度：" + score + "/200（" + familiarityEnum.getDesc() + "）" + desc;
    }

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
