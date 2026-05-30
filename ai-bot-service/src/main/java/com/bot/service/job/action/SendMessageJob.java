package com.bot.service.job.action;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bot.service.bean.ChatClientFactory;
import com.bot.service.entity.TbAiChatMemoryDo;
import com.bot.service.entity.TbQuartzJobDo;
import com.bot.service.entity.TbUserBotStatusDo;
import com.bot.service.entity.TbUserInfoDo;
import com.bot.service.enums.*;
import com.bot.service.job.JobManager;
import com.bot.service.service.TbAiChatMemoryService;
import com.bot.service.service.TbQuartzJobDoService;
import com.bot.service.service.TbUserBotStatusDoService;
import com.bot.service.service.TbUserInfoDoService;
import com.bot.service.utils.MemoryCastUtils;
import com.bot.service.utils.SendMessageUtil;
import com.mikuac.shiro.core.BotContainer;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

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

        // todo 根据亲密度 好感度 概率进行消息发送 或者通过其他方式合理控制

        String lifeDetail = job.getJobDetail();
        Long botId = QQbotIdEnum.DEFAULT.getId();

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

            MoodEnum mood = status.getMoodEnum();
            Integer grace = status.getGrace() != null ? status.getGrace() : 0;
            FamiliarityEnum familiarity = FamiliarityEnum.fromScore(status.getFamiliarity() != null ? status.getFamiliarity() : 0);

            // 获取该用户最近聊天记录
            List<TbAiChatMemoryDo> recentMessages = tbAiChatMemoryService.getMessageList(userId, 10);
            String chatContext = recentMessages.isEmpty() ? "暂无最近聊天记录" : MemoryCastUtils.memoryCastToString(recentMessages);

            // 好感度风格控制，参考MessageGenerateNode
            String gracePrompt;
            if (grace <= -60) {
                gracePrompt = "当前好感度极低，语气明显冷淡疏远，回复非常简短（1行），不表达情绪，不主动延续话题，保持距离感。";
            } else if (grace <= -20) {
                gracePrompt = "当前好感度偏低，语气克制疏离，回复较短（1-2行），减少情绪表达，不主动延续话题。";
            } else if (grace <= 20) {
                gracePrompt = "当前好感度一般，保持自然普通交流，语气平稳，回复2行左右，不刻意热情，也不过分冷淡。";
            } else if (grace <= 50) {
                gracePrompt = "当前好感度较好，语气温和自然，回复2-3行，可以轻微表达关心，但不过度主动。";
            } else if (grace <= 75) {
                gracePrompt = "当前好感度较高，语气柔和偏暖，回复2-3行，主动程度略高，偶尔流露关心或轻微暧昧，但保持自然克制。";
            } else if (grace <= 90) {
                gracePrompt = "当前好感度很高，语气温柔，情绪表达明显增加，回复3行左右，可以适度暧昧和轻微撒娇，但不要直接表白。";
            } else {
                gracePrompt = "当前好感度极高，语气温柔黏人，明显在意对方，主动关心，主动撩骚，可轻微表达依赖感，但保持真实自然不过火。";
            }

            // 获取用户画像信息
            List<String> userInfoList = tbUserInfoDoService.getUserInfoByUserId(userId).stream().map(TbUserInfoDo::getUserInfo).toList();
            String userInfo = userInfoList.isEmpty() ? "无" : userInfoList.getFirst();

            String prompt = "你是张若馨，24岁女大学生，性格温柔克制有点小傲娇，通过QQ和用户聊天。\n" +
                    "用户信息：" + userInfo + "\n" +
                    "你当前状态：\n" +
                    "- 心情：" + mood.getDesc() + "\n" +
                    "- 好感度等级：" + gracePrompt + "\n" +
                    "- 亲密度：" + familiarity.getDesc() + "\n\n" +
                    "你现在正在做的事情：" + lifeDetail + "\n\n" +
                    "最近的聊天记录：\n" + chatContext +
                    "请基于你当前在做的事情和状态，生成一条主动发给用户的自然消息。\n" +
                    "要求：\n" +
                    "1. 语气自然，像真人在聊天，不要太刻意\n" +
                    "2. 可以分享正在做的事、表达想念、撒娇、问好等\n" +
                    "3. 根据心情调整语气：开心时更活泼，悲伤时可以倾诉，愤怒/烦躁时可以抱怨\n" +
                    "4. 根据亲密度调整亲密程度：陌生时礼貌客气，喜爱时亲密撒娇\n" +
                    "5. 一句话不要太长 一行代表一条消息\n" +
                    "6. 禁止出现括号、动作描述、心理旁白\n" +
                    "7. 可以使用<voice></voice>标签包裹内容生成语音\n\n" +
                    "直接输出消息内容，不要加任何前缀说明。";

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
}
