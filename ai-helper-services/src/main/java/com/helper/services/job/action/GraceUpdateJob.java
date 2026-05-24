package com.helper.services.job.action;

import com.helper.services.bean.ChatClientFactory;
import com.helper.services.entity.TbUserMessageSummaryDo;
import com.helper.services.enums.NodeEnum;
import com.helper.services.service.TbAiChatMemoryService;
import com.helper.services.service.TbUpdateGraceHistoryDoService;
import com.helper.services.service.TbUserGraceDoService;
import com.helper.services.service.TbUserMessageSummaryDoService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GraceUpdateJob implements Job {

    @Autowired
    private TbUserGraceDoService tbUserGraceDoService;

    @Autowired
    private TbUpdateGraceHistoryDoService tbUpdateGraceHistoryDoService;

    @Autowired
    private TbAiChatMemoryService tbAiChatMemoryService;

    @Autowired
    private ChatClientFactory chatClientFactory;


    @Override
    public void execute(JobExecutionContext jobExecutionContext){
        // 查询tbUserGraceDo 表里面都是有哪些userId

        // 查询每一个userId今天的所有消息 tbAiChatMemoryService

        // 如果消息是空 好感度逐渐趋于0 如果当前好感度小于0就加一，如果大于0就减一

        // 如果消息不是空 用ai评分，最大加五最小减五 chatClientFactory
        ChatClient chatClient = chatClientFactory.createChatClient(userId, NodeEnum.WANT_TO.getNodeNum());
        String content = chatClient.prompt(prompt)
                .messages(messageList).call().content();

        // 修改数据库的好感度和好感度修改历史 tbUserGraceDoService tbUpdateGraceHistoryDoService
    }

}
