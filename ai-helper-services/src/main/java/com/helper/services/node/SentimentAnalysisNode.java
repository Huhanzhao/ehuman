package com.helper.services.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.helper.commonservicescommon.entity.BusinessException;
import com.helper.services.config.ChatClientFactory;
import com.helper.services.enums.ChatModelEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class SentimentAnalysisNode implements AsyncNodeAction {

    private final  ChatClientFactory chatClientFactory;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        String userMessage = (String)state.value("userInput").orElse("");
        String messageMemory = (String)state.value("messageMemory").orElse("");
        if (userMessage.isEmpty()){
            throw new BusinessException("用户输入读取失败");
        }
        log.info("用户输入:{}" , userMessage);
        ChatClient chatClient = chatClientFactory.createChatClient(ChatModelEnum.GPT_4O_MINI);
        String content = chatClient.prompt("""
                        # 你是情绪分析专家，你需要根据用户历史对话记录来判断用户当前情绪，
                        ## 要求：
                        * 1.只需要回复我枚举类型的值：
                        例如：Mad，Happy，Sad，Excited，Anxious... 
                        * 2.如果有这些之外的情绪你也可以自行添加枚举值。
                        * 3.假设每种情绪有0~1的程度，请给出用户情绪程度分值。
                        * 4.严格按照以下例子的格式输出,需要给我的结果例如：
                            例子1：Mad(强度：0.7)、
                            例子2：Sad(强度：0.6)
                            ...
                        ## 以下是你和用户的历史对话\n
                        """ + messageMemory)
                .user(userMessage).call().content();
        log.info("用户对话情绪：{}" , content);
        return CompletableFuture.completedFuture(Map.of("sentiment", "用户情绪：" + content));
    }
}
