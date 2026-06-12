package com.anyfriend.core.job;

import com.anyfriend.dao.entity.TbQuartzJobDo;
import com.anyfriend.dao.service.TbQuartzJobDoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.PostConstruct;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JobManager {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private TbQuartzJobDoService quartzJobService;

    public void addJob(String jobName, String jobGroup, String cron, String className, String jobDetail) throws Exception {
        // 保存到数据库
        TbQuartzJobDo job = new TbQuartzJobDo();
        job.setJobName(jobName);
        job.setJobGroup(jobGroup);
        job.setCron(cron);
        job.setClassName(className);
        job.setJobDetail(jobDetail);
        job.setStatus(true);
        quartzJobService.save(job);

        // 添加到Quartz调度器
        scheduleJob(jobName, jobGroup, cron, className);
    }

    public void deleteJob(String jobName, String jobGroup) throws Exception {
        // 从数据库删除
        quartzJobService.remove(new LambdaQueryWrapper<TbQuartzJobDo>()
                .eq(TbQuartzJobDo::getJobName, jobName)
                .eq(TbQuartzJobDo::getJobGroup, jobGroup));

        // 从Quartz调度器删除
        scheduler.deleteJob(new JobKey(jobName, jobGroup));
    }

    public void updateJob(String jobName, String jobGroup, String cron) throws Exception {
        // 更新数据库中的cron表达式
        quartzJobService.update(new LambdaUpdateWrapper<TbQuartzJobDo>()
                .eq(TbQuartzJobDo::getJobName, jobName)
                .eq(TbQuartzJobDo::getJobGroup, jobGroup)
                .set(TbQuartzJobDo::getCron, cron));

        // 更新Quartz调度器中的触发器
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName + "Trigger", jobGroup);
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .withSchedule(scheduleBuilder)
                .build();
        scheduler.rescheduleJob(triggerKey, trigger);
    }

    public void pauseJob(String jobName, String jobGroup) throws Exception {
        // 更新数据库状态为暂停
        quartzJobService.update(new LambdaUpdateWrapper<TbQuartzJobDo>()
                .eq(TbQuartzJobDo::getJobName, jobName)
                .eq(TbQuartzJobDo::getJobGroup, jobGroup)
                .set(TbQuartzJobDo::getStatus, false));

        // 暂停Quartz调度器中的任务
        scheduler.pauseJob(new JobKey(jobName, jobGroup));
    }

    public void resumeJob(String jobName, String jobGroup) throws Exception {
        // 更新数据库状态为运行中
        quartzJobService.update(new LambdaUpdateWrapper<TbQuartzJobDo>()
                .eq(TbQuartzJobDo::getJobName, jobName)
                .eq(TbQuartzJobDo::getJobGroup, jobGroup)
                .set(TbQuartzJobDo::getStatus, true));

        // 恢复Quartz调度器中的任务
        scheduler.resumeJob(new JobKey(jobName, jobGroup));
    }

    @PostConstruct
    public void init() throws Exception {
        // 从数据库中加载所有任务
        List<TbQuartzJobDo> jobs = quartzJobService.list();

        for (TbQuartzJobDo job : jobs) {
            if (Boolean.TRUE.equals(job.getStatus())) {
                scheduleJob(job.getJobName(), job.getJobGroup(), job.getCron(), job.getClassName());
            }
        }
    }

    private void scheduleJob(String jobName, String jobGroup, String cron, String className) throws Exception {
        Class<? extends Job> jobClass = (Class<? extends Job>) Class.forName(className);

        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(jobName, jobGroup)
                .build();

        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobName + "Trigger", jobGroup)
                .withSchedule(scheduleBuilder)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }
}
