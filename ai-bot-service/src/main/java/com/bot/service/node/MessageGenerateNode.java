package com.bot.service.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson2.util.DateUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bot.service.bean.ChatClientFactory;
import com.bot.service.entity.TbAiChatMemoryDo;
import com.bot.service.entity.TbBotLifeDetailDo;
import com.bot.service.entity.TbUserInfoDo;
import com.bot.service.entity.TbUserMessageSummaryDo;
import com.bot.service.enums.FamiliarityEnum;
import com.bot.service.enums.MoodEnum;
import com.bot.service.enums.NodeEnum;
import com.bot.service.enums.PromptTypeEnum;
import com.bot.service.service.*;
import com.bot.service.utils.MemoryCastUtils;
import com.bot.service.utils.SendMessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Component
public class MessageGenerateNode implements NodeAction {

    private final ChatClientFactory chatClientFactory;

    private final TbAiChatMemoryService tbAiChatMemoryService;

    private final TbUserInfoDoService tbUserInfoDoService;

    private final TbBotLifeDetailDoService tbBotLifeDetailDoService;

    private final TbUserMessageSummaryDoService tbUserMessageSummaryDoService;

    private final SendMessageUtil sendMessageUtil;

    private final TbBotPromptDoService tbBotPromptDoService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        Long userId = (Long)state.value("userId").orElse(0L);
        Boolean initiative = (Boolean)state.value("initiative").orElse(false);
        List<Message> messageList = MemoryCastUtils.memoryCastToMessages(
                tbAiChatMemoryService.getMessageList(userId, 15));
        ChatClient chatClient = chatClientFactory.createChatClient(userId, NodeEnum.GENERATE);

        String template = null;
        try {
            template = Files.readString(Paths.get("./md/MainPrompt.md"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        PromptTemplate promptTemplate = PromptTemplate.builder().template(template).build();

        // 获取提示词变量
        Prompt prompt = promptTemplate.create(getPromptVariables(state));

        log.info("提示词：{}" , prompt);
        ChatClient.ChatClientRequestSpec chatClientRequestSpec = chatClient.prompt(prompt)
                .messages(messageList);
        String content = null;
        if ( initiative){
            content = chatClientRequestSpec.user("请根据提示生成消息").call().content();
        }else{
            content = chatClientRequestSpec.call().content();
        }
        log.info("结果输出：{}" , content);
        sendMessageUtil.sendChatMsg(userId,content);
        return Map.of();
    }

    private Map getPromptVariables(OverAllState state) {
        // 获取bot状态
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
        TbBotLifeDetailDo currentLife = tbBotLifeDetailDoService.getOne(
                Wrappers.<TbBotLifeDetailDo>lambdaQuery()
                        .le(TbBotLifeDetailDo::getStartTime, now)
                        .ge(TbBotLifeDetailDo::getEndTime, now)
                        .last("limit 1")
        );
        String lifeDetail = currentLife != null ? currentLife.getDetail() : "";

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
        TbAiChatMemoryDo lastUserMessage = tbAiChatMemoryService.getOne(
                Wrappers.<TbAiChatMemoryDo>lambdaQuery()
                        .eq(TbAiChatMemoryDo::getMemoryId, userId)
                        .eq(TbAiChatMemoryDo::getRole, "user")
                        .orderByDesc(TbAiChatMemoryDo::getCreateTime)
                        .last("limit 1")
        );
        String lastMessageTime = "";
        if (lastUserMessage != null && lastUserMessage.getCreateTime() != null) {
            lastMessageTime = "用户最后一条消息是" + DateUtils.format(lastUserMessage.getCreateTime(), "MM月dd日 HH:mm");
        }

        // 构建时间信息
        String currentTime = "今天是" + DateUtils.format(now, "yyyy年") + LocalDateTime.now().getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.CHINA)
                + "，今天是" + DateUtils.format(now, "MM月dd日 HH:mm")
                + (lastMessageTime.isEmpty() ? "" : "，" + lastMessageTime);

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

        return Map.of(
                "botRole",botRole,
                "target",target,
                "mood", moodPrompt,
                "familiarity", familiarityPrompt,
                "grace", gracePrompt,
                "currentTime", currentTime,
                "daysTogether", daysInfo,
                "currentActivity", lifeDetail.isBlank() ? "" : "你现在正在" + lifeDetail,
                "userInfo", userInfoList.isEmpty() ? "无" : userInfoList.getFirst(),
                "yesterdayChatSummary", yesterdayChatSummary
        );
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
