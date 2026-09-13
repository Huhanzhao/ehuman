package com.anyfriend.core.job.action;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.anyfriend.core.job.JobManager;
import com.anyfriend.core.utils.LocalCacheUtil;
import com.anyfriend.dao.entity.TbQuartzJobDo;
import com.anyfriend.dao.entity.TbUserBotStatusDo;
import com.anyfriend.dao.service.TbAiChatMemoryService;
import com.anyfriend.dao.service.TbQuartzJobDoService;
import com.anyfriend.dao.service.TbUserBotStatusDoService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

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

    private LocalCacheUtil localCacheUtil = LocalCacheUtil.getLocalCacheUtil();

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
        TbQuartzJobDo tbQuartzJobDo = tbQuartzJobDoService.getByStartName(jobName);

        for (TbUserBotStatusDo status : allStatus) {
            // 判断是不是有正在运行的图 有的话取消发送消息
            Long userId = status.getUserId();
            if (localCacheUtil.containsKey(userId + "_Graph")){
                return;
            }

            String chatId = UUID.randomUUID().toString();
            // 配置线程 ID
            var invokeConfig = RunnableConfig.builder()
                    .threadId(chatId)
                    .build();
            Flux<NodeOutput> stream = sendMessageCompiledGraph.stream(
                    Map.of("userId", userId, "initiative", true, "topic", tbQuartzJobDo.getJobDetail()),
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
