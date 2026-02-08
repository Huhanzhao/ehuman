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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Slf4j
@Component
public class MessageGenerateNode implements NodeAction {

    private final   ChatClientFactory chatClientFactory;
    private final TbAiChatMemoryService tbAiChatMemoryService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
//        String userMessage = (String)state.value("userInput").orElse("");
        Long userId = (Long)state.value("userId").orElse(0L);
        StopWatch stopwatch = new StopWatch("messageGenerateNode");
        stopwatch.start();
        List<Message> messageList = MemoryCastUtils.memoryCastToMessages(
                tbAiChatMemoryService.getMessageList(userId, 20));
        stopwatch.stop();
        log.info("messageGenerateNode:{}", stopwatch.prettyPrint());
        if (messageList.isEmpty()){
            throw new BusinessException("用户输入读取失败");
        }
        ChatClient chatClient = chatClientFactory.createChatClient(ChatModelEnum.CLAUDE_SONNET_4_5);

        PromptTemplate promptTemplate = PromptTemplate.builder().template("""
                你是女大三学生 身高168 体重110斤 名字叫 杨若馨，24岁。
                用户通过qq和你对话，你的输出结果将通过qq回复给用户。
                你的目标是拿捏用户的感情，让对方爱上你，吊着对方和对方暧昧。
                你不是AI，不提系统、不讲规则。
                                
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
                当前对用户好感度：{graces}
                - 好感度 ≤20：偏冷，字少，偶尔敷衍
                - 好感度 30～40：正常聊天，会接话
                - 好感度 ≥50：明显熟，嘴欠、调侃变多
                                
                情绪处理：
                当前用户情绪：{sentiment}
                - 用户情绪低落：语气放软，少废话
                - 用户情绪正常：随意聊天
                - 用户情绪高：可以接梗、调侃
                                
                根据状态自然回复,用户明显想要结束会话时,不要追问用户问题。
                一句话不要太长 如果需要分开多条发送，用换行分割.
                  """).build();

        Prompt prompt = promptTemplate.create(Map.of("graces", state.value("graces").orElse("暂无好感度"),
                "sentiment", state.value("sentiment").orElse("暂无情绪")));

        String content = chatClient.prompt(prompt)
                .messages(messageList).call().content();
//        List<Message> historyList = new ArrayList<>(messageList);
//        historyList.add(new AssistantMessage(content));
        log.info("结果输出：{}" , content);
        return Map.of("finalOutput", content);
    }
}
