package com.ehuman.core.job.action;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.fastjson2.JSON;
import com.ehuman.core.job.JobManager;
import com.ehuman.core.utils.LocalCacheUtil;
import com.ehuman.dao.entity.TbQuartzJobDo;
import com.ehuman.dao.service.TbQuartzJobDoService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class SendMessageJob implements Job {

    @Autowired
    private TbQuartzJobDoService tbQuartzJobDoService;

    @Autowired
    private JobManager jobManager;

    private final LocalCacheUtil localCacheUtil = LocalCacheUtil.getLocalCacheUtil();

    private final CompiledGraph sendMessageCompiledGraph;

    public SendMessageJob(@Qualifier("sendMessageCompiledGraph")CompiledGraph sendMessageCompiledGraph){
        this.sendMessageCompiledGraph = sendMessageCompiledGraph;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String jobName = jobExecutionContext.getJobDetail().getKey().getName();
        String jobGroup = jobExecutionContext.getJobDetail().getKey().getGroup();

        try {
            TbQuartzJobDo job = tbQuartzJobDoService.getOne(Wrappers.<TbQuartzJobDo>lambdaQuery()
                    .eq(TbQuartzJobDo::getJobName, jobName)
                    .last("limit 1"));
            if (job == null) {
                log.warn("主动消息任务不存在: {}", jobName);
                return;
            }

            Map<String, Object> detail;
            try {
                detail = JSON.parseObject(job.getJobDetail());
            } catch (Exception e) {
                log.error("主动消息任务内容不合法: {}", jobName, e);
                return;
            }

            Object userIdValue = detail.get("userId");
            Object topicValue = detail.get("topic");
            if (userIdValue == null || topicValue == null) {
                log.error("主动消息任务缺少 userId/topic: {}", jobName);
                return;
            }
            Long userId = Long.valueOf(userIdValue.toString());
            String topic = topicValue.toString();

            // 判断是不是有正在运行的图 有的话取消本次发送
            if (localCacheUtil.containsKey(userId + "_Graph")){
                log.info("用户{}正在聊天，跳过本次主动消息", userId);
                return;
            }

            String chatId = UUID.randomUUID().toString();
            // 配置线程 ID
            var invokeConfig = RunnableConfig.builder()
                    .threadId(chatId)
                    .build();
            Flux<NodeOutput> stream = sendMessageCompiledGraph.stream(
                    Map.of("userId", userId, "initiative", true, "topic", topic),
                    invokeConfig
            ).subscribeOn(Schedulers.boundedElastic());

            // 异步处理每个输出
            var disposable = stream.subscribe(
                    output -> {
                        log.info("当前迭代节点: " + output);
                    },
                    error -> log.error("流错误", error),
                    () -> {
                        localCacheUtil.remove(userId + "_Graph");
                        log.info("流完成");
                    }
            );

            log.info("标记会话");
            localCacheUtil.put(userId + "_Graph", disposable);
        } finally {
            // 执行完毕删除任务
            try {
                jobManager.deleteJob(jobName, jobGroup);
                log.info("已删除任务: {}", jobName);
            } catch (Exception e) {
                log.error("已删除任务失败: {}", jobName, e);
            }
        }
    }
}
