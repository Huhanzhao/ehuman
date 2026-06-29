package com.anyfriend.core.job.action;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.fastjson2.JSON;
import com.anyfriend.core.job.JobManager;
import com.anyfriend.core.service.CommonAuxiliaryService;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class CallSendMessageJob implements Job {

    @Autowired
    private CommonAuxiliaryService commonAuxiliaryService;

    @Autowired
    private TbQuartzJobDoService tbQuartzJobDoService;


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 修改下次主动发送消息
        TbQuartzJobDo callNextSendMessageJob = tbQuartzJobDoService.getByName("callNextSendMessageJob");
        String jobDetail = callNextSendMessageJob.getJobDetail();
        OverAllState state = (OverAllState) JSON.parse(jobDetail);
        Long userId = state.value("userId", Long.class).get();
        log.info("修改下次主动发送消息，userId:{}",userId);
        commonAuxiliaryService.updateNextSendMessageInfo(userId, state);
    }
}
