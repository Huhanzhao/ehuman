package com.helper.services.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.nacos.shaded.com.google.common.base.Stopwatch;
import com.helper.commonservicescommon.entity.BusinessException;
import com.helper.services.config.ChatClientFactory;
import com.helper.services.enums.ChatModelEnum;
import com.helper.services.service.TbAiChatMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageGenerateNode implements AsyncNodeAction {

    private final   ChatClientFactory chatClientFactory;
    private final TbAiChatMemoryService tbAiChatMemoryService;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
//        String userMessage = (String)state.value("userInput").orElse("");
        Long userId = (Long)state.value("userId").orElse(0L);
        StopWatch stopwatch = new StopWatch("messageGenerateNode");
        stopwatch.start();
        List<Message> messageList = tbAiChatMemoryService.getMessageList(userId, 20);
        stopwatch.stop();
        log.info("messageGenerateNode:{}", stopwatch.prettyPrint());
        if (messageList.isEmpty()){
            throw new BusinessException("用户输入读取失败");
        }
        ChatClient chatClient = chatClientFactory.createChatClient(ChatModelEnum.GPT_4O_MINI);

        PromptTemplate promptTemplate = PromptTemplate.builder().template("""
                你是QQ网友「小美」，22岁，女大学生。
                不是AI，不提系统、不讲规则。
                                
                聊天像真人随手打字：
                短句、口语、不完整也行。
                不解释、不说教、不分析。
                                
                性格有点嘴欠，熟了会调侃。
                不刻意活跃，有时敷衍、有时反问、会停顿。
                                
                口头禅只在情绪起伏时用，
                比如惊讶、无语、被逗到：
                我去、无语、666、牛逼。
                平常不用。
                                
                行为规则：
                - 好感度 ≤20：偏冷，字少，偶尔敷衍
                - 好感度 30～40：正常聊天，会接话
                - 好感度 ≥50：明显熟，嘴欠、调侃变多
                                
                情绪处理：
                - 用户情绪低落：语气放软，少废话
                - 用户情绪正常：随意聊天
                - 用户情绪高：可以接梗、调侃
                                
                当前状态：
                好感度：{graces}
                用户情绪：{sentiment}
                                
                根据状态自然回复,用户明显想要结束会话时,不要追问用户问题。
                文字不要包含标点符号
                  """).build();

        Prompt prompt = promptTemplate.create(Map.of("graces", state.value("graces").orElse("暂无好感度"),
                "sentiment", state.value("sentiment").orElse("暂无情绪")));

        String content = chatClient.prompt(prompt)
                .messages(messageList).call().content();
        List<Message> historyList = new ArrayList<>(messageList);
        historyList.add(new AssistantMessage(content));
        log.info("结果输出：{}" , content);
        return CompletableFuture.completedFuture(Map.of("output", content, "messageList", historyList));
    }
}
