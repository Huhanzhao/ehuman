package com.helper.services.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.helper.commonservicescommon.entity.BusinessException;
import com.helper.services.bean.ChatClientFactory;
import com.helper.services.enums.ChatModelEnum;
import com.helper.services.service.TbAiChatMemoryService;
import com.helper.services.utils.MemoryCastUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


@RequiredArgsConstructor
@Slf4j
@Component
public class SentimentAnalysisNode implements NodeAction {

    private final  ChatClientFactory chatClientFactory;

    private final TbAiChatMemoryService tbAiChatMemoryService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String userMessage = (String)state.value("userInput").orElse("");
        String memoryString = (String)state.value("memoryString").orElse("");
        Long userId = (Long)state.value("userId").orElse(0L);
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
                            例子1：Mad(强度：0.7,范围0~1)、
                            例子2：Sad(强度：0.6,范围0~1)
                            ...
                        """)
                .user(memoryString + "\n 以上是用户对话记录，请分许用户情绪").call().content();
        log.info("用户对话情绪：{}" , content);
        return Map.of("sentiment", "用户情绪：" + content);
    }
}
