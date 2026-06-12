package com.anyfriend.core.job.action;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.anyfriend.core.job.JobManager;
import com.anyfriend.dao.entity.TbAiChatMemoryDo;
import com.anyfriend.dao.entity.TbUserBotStatusDo;
import com.anyfriend.dao.service.TbAiChatMemoryService;
import com.anyfriend.dao.service.TbUserBotStatusDoService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class SendMessageJob implements Job {

    @Autowired
    private TbUserBotStatusDoService tbUserBotStatusDoService;

    @Autowired
    private TbAiChatMemoryService tbAiChatMemoryService;

    @Autowired
    private JobManager jobManager;

    private final CompiledGraph sendMessageCompiledGraph;

    public SendMessageJob(@Qualifier("sendMessageCompiledGraph")CompiledGraph sendMessageCompiledGraph){
        this.sendMessageCompiledGraph = sendMessageCompiledGraph;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String jobName = jobExecutionContext.getJobDetail().getKey().getName();
        String jobGroup = jobExecutionContext.getJobDetail().getKey().getGroup();

        List<TbUserBotStatusDo> allStatus = tbUserBotStatusDoService.list();
        if (allStatus == null || allStatus.isEmpty()) {
            return;
        }

        Date threeDaysAgo = Date.from(LocalDate.now().minusDays(3).atStartOfDay(ZoneId.systemDefault()).toInstant());
//        Date tenMinutesAgo = new Date(System.currentTimeMillis() - 10 * 60 * 1000);

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
//            long recentTenMinCount = tbAiChatMemoryService.count(
//                    Wrappers.<TbAiChatMemoryDo>lambdaQuery()
//                            .eq(TbAiChatMemoryDo::getMemoryId, userId)
//                            .ge(TbAiChatMemoryDo::getCreateTime, tenMinutesAgo)
//            );
//            if (recentTenMinCount > 0) {
//                log.info("用户{}十分钟内有对话，跳过主动消息", userId);
//                continue;
//            }

            String target = """
            ## 目标：请基于你当前在做的事情和状态还有历史消息，生成一条主动发给用户的自然消息,一行或者两行。
            不要太突兀，要与上下文相和谐
            1. 可以分享正在做的事、表达想念、撒娇、问好等。
            """;
            String chatId = UUID.randomUUID().toString();
            // 配置线程 ID
            var invokeConfig = RunnableConfig.builder()
                    .threadId(chatId)
                    .build();
            sendMessageCompiledGraph.invoke(
                    Map.of("userId", userId, "target", target, "initiative", true),
                    invokeConfig
            );
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
