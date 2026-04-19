package com.helper.services.job;

import com.helper.services.job.action.MyQuartzJob;
import jakarta.annotation.PostConstruct;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JobManager {

    @Autowired
    private Scheduler scheduler;

    public void addJob(String jobName, String jobGroup, String cron) throws Exception {

        // todo 添加任务到数据库

        JobDetail jobDetail = JobBuilder.newJob(MyQuartzJob.class)
                .withIdentity(jobName, jobGroup)
                .build();

        CronScheduleBuilder scheduleBuilder =
                CronScheduleBuilder.cronSchedule(cron);

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobName + "Trigger", jobGroup)
                .withSchedule(scheduleBuilder)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }

    public void deleteJob(String jobName, String jobGroup) throws Exception {
        // 删除 todo 删除数据库中的任务
        scheduler.deleteJob(new JobKey(jobName, jobGroup));
    }

    public void updateJob(String jobName, String jobGroup, String cron) throws Exception {

        TriggerKey triggerKey = TriggerKey.triggerKey(jobName + "Trigger", jobGroup);

        CronScheduleBuilder scheduleBuilder =
                CronScheduleBuilder.cronSchedule(cron);

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .withSchedule(scheduleBuilder)
                .build();

        scheduler.rescheduleJob(triggerKey, trigger);

        // todo 修改数据库中的状态
    }

    public void pauseJob(String jobName, String jobGroup) throws Exception {
        // 暂停 todo 修改数据库中的状态
        scheduler.pauseJob(new JobKey(jobName, jobGroup));
    }

    public void resumeJob(String jobName, String jobGroup) throws Exception {
        // 恢复  todo 修改数据库中的状态
        scheduler.resumeJob(new JobKey(jobName, jobGroup));
    }

    @PostConstruct
    public void init() throws Exception {

        // todo 从数据库中加载任务
//        List<JobEntity> jobs = jobMapper.selectAll();
//
//        for (JobEntity job : jobs) {
//            if (job.getStatus() == 1) {
//                addJob(job.getJobName(), job.getJobGroup(), job.getCronExpression());
//            }
//        }
        addJob("发送腾讯新闻消息", "sendMessage", "0 0 9 * * ?");
    }


}
