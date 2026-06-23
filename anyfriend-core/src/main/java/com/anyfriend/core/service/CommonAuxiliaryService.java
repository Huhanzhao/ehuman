package com.anyfriend.core.service;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.anyfriend.core.bean.ChatClientFactory;
import com.anyfriend.core.job.JobManager;
import com.anyfriend.dao.enums.NodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Service
public class CommonAuxiliaryService {

    @Autowired
    private JobManager jobManager;

    @Autowired
    private ChatClientFactory chatClientFactory;

    // todo 更新下次主动发送消息的信息
    @Transactional
    public String updateNextSendMessageInfo(Long userId, OverAllState state) {
        ChatClient chatClient = chatClientFactory.createChatClient(userId, NodeEnum.AUXILIARY);
        PromptTemplate promptTemplate = null;
        try {
            promptTemplate = PromptTemplate.builder().template(
                    Files.readString(Paths.get("./md/NextSendMessage.md"))).build();
        } catch (IOException e) {
            log.error("读取文件异常");
            throw new RuntimeException("读取文件异常");
        }

        String prompt = promptTemplate.render(state.data());
        String output = chatClient.prompt(prompt).call().content();

        // todo json解析 output 更新定时任务 补充提示词 定时任务执行类

        try {
            jobManager.deleteJob("nextSendMessage", "system");
            jobManager.addJob("nextSendMessage", "system", "corn", "className","jobDetail");
        } catch (Exception e) {
            log.error("更新定时任务失败");
            throw new RuntimeException("更新定时任务失败");
        }
        return "";
    }
}
