package com.bot.service.job.action;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Component
public class SendMessageJob implements Job {

    // todo 主动发送消息定时任务 判断与当前上下文是否冲突 时间 语言 心情 好感度
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

    }
}
