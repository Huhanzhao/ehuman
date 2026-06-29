package com.anyfriend.core.job.action;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.fastjson2.JSON;
import com.anyfriend.core.service.CommonAuxiliaryService;
import com.anyfriend.dao.entity.TbQuartzJobDo;
import com.anyfriend.dao.service.TbQuartzJobDoService;
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


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 修改下次主动发送消息
        TbQuartzJobDo callNextSendMessageJob = tbQuartzJobDoService.getByName("callNextSendMessageJob");
        String jobDetail = callNextSendMessageJob.getJobDetail();
        Map<String,Object> data = (Map<String,Object>) JSON.parse(jobDetail);
        OverAllState state = new OverAllState(data);
        Long userId = state.value("userId", Long.class).get();
        log.info("修改下次主动发送消息，userId:{}",userId);
        commonAuxiliaryService.updateNextSendMessageInfo(userId, state);
    }
}
