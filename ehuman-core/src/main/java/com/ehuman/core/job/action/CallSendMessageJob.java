package com.ehuman.core.job.action;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.fastjson2.JSON;
import com.ehuman.core.job.JobManager;
import com.ehuman.core.service.CommonAuxiliaryService;
import com.ehuman.dao.entity.TbQuartzJobDo;
import com.ehuman.dao.service.TbQuartzJobDoService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class CallSendMessageJob implements Job {

    @Autowired
    private CommonAuxiliaryService commonAuxiliaryService;

    @Autowired
    private TbQuartzJobDoService tbQuartzJobDoService;

    @Autowired
    private JobManager jobManager;


    @Override
    @SuppressWarnings("unchecked")
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String jobName = jobExecutionContext.getJobDetail().getKey().getName();
        String jobGroup = jobExecutionContext.getJobDetail().getKey().getGroup();
        try {
            // 按任务名精确取当前用户的任务，避免多用户时串号
            TbQuartzJobDo job = tbQuartzJobDoService.getOne(Wrappers.<TbQuartzJobDo>lambdaQuery()
                    .eq(TbQuartzJobDo::getJobName, jobName)
                    .last("limit 1"));
            if (job == null) {
                log.warn("主动消息任务不存在: {}", jobName);
                return;
            }

            Map<String, Object> data;
            try {
                data = (Map<String, Object>) JSON.parse(job.getJobDetail());
            } catch (Exception e) {
                log.error("主动消息任务内容不合法，删除任务: {}", jobName, e);
                jobManager.deleteJob(jobName, jobGroup);
                return;
            }

            OverAllState state = new OverAllState(data);
            Long userId = state.value("userId", Long.class).orElse(null);
            if (userId == null) {
                log.error("主动消息任务缺少 userId，删除任务: {}", jobName);
                jobManager.deleteJob(jobName, jobGroup);
                return;
            }

            // 修改下次主动发送消息
            log.info("修改下次主动发送消息，userId:{}", userId);
            commonAuxiliaryService.updateNextSendMessageInfo(userId, state);
        } catch (Exception e) {
            log.error("执行主动消息任务失败: {}", jobName, e);
        } finally {
            try {
                jobManager.deleteJob(jobName, jobGroup);
            } catch (Exception e) {
                log.error("删除主动消息任务失败: {}", jobName, e);
            }
        }
    }
}
