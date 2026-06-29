package com.anyfriend.core.job.action;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.anyfriend.core.job.JobManager;
import com.anyfriend.dao.entity.TbAiChatMemoryDo;
import com.anyfriend.dao.entity.TbQuartzJobDo;
import com.anyfriend.dao.entity.TbUserBotStatusDo;
import com.anyfriend.dao.service.TbAiChatMemoryService;
import com.anyfriend.dao.service.TbQuartzJobDoService;
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
    private TbQuartzJobDoService tbQuartzJobDoService;

    @Autowired
    private JobManager jobManager;

    private final CompiledGraph sendMessageCompiledGraph;

    public SendMessageJob(@Qualifier("sendMessageCompiledGraph")CompiledGraph sendMessageCompiledGraph){
        this.sendMessageCompiledGraph = sendMessageCompiledGraph;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String jobName = jobExecutionContext.getJobDetail().getKey().getName();  // todo 数据库增加唯一键
        String jobGroup = jobExecutionContext.getJobDetail().getKey().getGroup();

        List<TbUserBotStatusDo> allStatus = tbUserBotStatusDoService.list();
        if (allStatus == null || allStatus.isEmpty()) {
            return;
        }
        TbQuartzJobDo tbQuartzJobDo = tbQuartzJobDoService.getByName(jobName);

        for (TbUserBotStatusDo status : allStatus) {
            Long userId = status.getUserId();

            String chatId = UUID.randomUUID().toString();
            // 配置线程 ID
            var invokeConfig = RunnableConfig.builder()
                    .threadId(chatId)
                    .build();
            sendMessageCompiledGraph.invoke(
                    Map.of("userId", userId,  "initiative", true,"topic",tbQuartzJobDo.getJobDetail()),
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
