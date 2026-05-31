package com.bot.service.job.action;

import com.alibaba.fastjson2.util.DateUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bot.service.bean.ChatClientFactory;
import com.bot.service.entity.*;
import com.bot.service.enums.*;
import com.bot.service.job.JobManager;
import com.bot.service.service.*;
import com.bot.service.utils.MemoryCastUtils;
import com.bot.service.utils.SendMessageUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SendMessageJob implements Job {

    @Autowired
    private TbQuartzJobDoService tbQuartzJobDoService;

    @Autowired
    private TbUserBotStatusDoService tbUserBotStatusDoService;

    @Autowired
    private TbAiChatMemoryService tbAiChatMemoryService;

    @Autowired
    private TbUserInfoDoService tbUserInfoDoService;

    @Autowired
    private TbUserMessageSummaryDoService tbUserMessageSummaryDoService;

    @Autowired
    private TbBotPromptDoService tbBotPromptDoService;

    @Autowired
    private ChatClientFactory chatClientFactory;

    @Autowired
    private JobManager jobManager;

    @Autowired
    private SendMessageUtil sendMessageUtil;


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String jobName = jobExecutionContext.getJobDetail().getKey().getName();
        String jobGroup = jobExecutionContext.getJobDetail().getKey().getGroup();

        TbQuartzJobDo job = tbQuartzJobDoService.getOne(
                Wrappers.<TbQuartzJobDo>lambdaQuery()
                        .eq(TbQuartzJobDo::getJobName, jobName)
                        .eq(TbQuartzJobDo::getJobGroup, jobGroup)
        );
        if (job == null || job.getJobDetail() == null) {
            log.warn("未找到定时任务详情: {}", jobName);
            return;
        }

        String lifeDetail = job.getJobDetail();

        List<TbUserBotStatusDo> allStatus = tbUserBotStatusDoService.list();
        if (allStatus == null || allStatus.isEmpty()) {
            return;
        }

        Date threeDaysAgo = Date.from(LocalDate.now().minusDays(3).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date tenMinutesAgo = new Date(System.currentTimeMillis() - 10 * 60 * 1000);

        for (TbUserBotStatusDo status : allStatus) {
            Long userId = status.getUserId();

            // 近三天没有聊天记录的用户跳过
            long recentCount = tbAiChatMemoryService.count(
                    Wrappers.<TbAiChatMemoryDo>lambdaQuery()
                            .eq(TbAiChatMemoryDo::getMemoryId, userId)
                            .eq(TbAiChatMemoryDo::getRole, "user")
                            .ge(TbAiChatMemoryDo::getCreateTime, threeDaysAgo)
            );
            if (recentCount == 0) {
                log.info("用户{}近三天无聊天记录，跳过主动消息", userId);
                continue;
            }

            // 十分钟内有对话则跳过
            long recentTenMinCount = tbAiChatMemoryService.count(
                    Wrappers.<TbAiChatMemoryDo>lambdaQuery()
                            .eq(TbAiChatMemoryDo::getMemoryId, userId)
                            .ge(TbAiChatMemoryDo::getCreateTime, tenMinutesAgo)
            );
            if (recentTenMinCount > 0) {
                log.info("用户{}十分钟内有对话，跳过主动消息", userId);
                continue;
            }

            Prompt prompt = getPrompt(lifeDetail, status);

            try {
                ChatClient chatClient = chatClientFactory.createChatClient(userId, NodeEnum.GENERATE);
                String message = chatClient.prompt(prompt).call().content();
                log.info("主动消息生成结果(userId={}): {}", userId, message);
                sendMessageUtil.sendChatMsg(userId, message);
            } catch (Exception e) {
                log.error("主动消息发送失败, userId={}", userId, e);
            }
        }

        // 执行完毕删除任务
        try {
            jobManager.deleteJob(jobName, jobGroup);
            log.info("已删除任务: {}", jobName);
        } catch (Exception e) {
            log.error("已删除任务失败: {}", jobName, e);
        }
    }

    private Prompt getPrompt(String lifeDetail, TbUserBotStatusDo status) {
        Long userId = status.getUserId();
        // 获取状态信息
        Integer moodCode = status.getMoodEnum() != null ? status.getMoodEnum().getCode() : 0;
        Integer grace = status.getGrace() != null ? status.getGrace() : 0;
        Integer familiarityScore = status.getFamiliarity() != null ? status.getFamiliarity() : 0;

        MoodEnum moodEnum = MoodEnum.fromCode(moodCode);
        FamiliarityEnum familiarityEnum = FamiliarityEnum.fromScore(familiarityScore);

        // 构建提示词变量
        String moodPrompt = buildMoodPrompt(moodEnum, "");
        String familiarityPrompt = buildFamiliarityPrompt(familiarityEnum, familiarityScore);
        String gracePrompt = buildGracePrompt(grace);

        // 获取用户画像信息
        List<String> userInfoList = tbUserInfoDoService.getUserInfoByUserId(userId).stream().map(TbUserInfoDo::getUserInfo).toList();
        String userInfo = userInfoList.isEmpty() ? "无" : userInfoList.getFirst();

        // 查询加上好友的天数
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
        Date now = new Date();
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

        // 获取bot角色设定
        String botRole = tbBotPromptDoService.getContext(PromptTypeEnum.BOT_ROLE);

        // 获取该用户最近聊天记录
        List<TbAiChatMemoryDo> recentMessages = tbAiChatMemoryService.getMessageList(userId, 10);
        String chatContext = recentMessages.isEmpty() ? "暂无最近聊天记录" : MemoryCastUtils.memoryCastToString(recentMessages);

        String template = null;
        try {
            template = Files.readString(Paths.get("./md/MainPrompt.md"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        PromptTemplate promptTemplate = PromptTemplate.builder().template(template).build();

        String target = """
            ## 目标：请基于你当前在做的事情和状态还有历史消息，生成一条主动发给用户的自然消息。
            1. 可以分享正在做的事、表达想念、撒娇、问好等。
            2. 根据状态使用标签生成语音，或者拒绝发送,也可以生成普通文字消息
            3. 如果用户多次不回复，可以生成<silence></silence>消息拒绝主动发消息
            """;
        // 获取提示词变量
        Prompt prompt = promptTemplate.create(Map.of(
                "botRole",botRole,
                "target",target,
                "mood", moodPrompt,
                "familiarity", familiarityPrompt,
                "grace", gracePrompt,
                "currentTime", currentTime,
                "daysTogether", daysInfo,
                "currentActivity", lifeDetail.isBlank() ? "" : "你现在正在" + lifeDetail  + "\n 历史对话消息："  + chatContext,
                "userInfo", userInfoList.isEmpty() ? "无" : userInfoList.getFirst(),
                "yesterdayChatSummary", yesterdayChatSummary
        ));
        return prompt;
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
