package com.helper.services.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.helper.commonservicescommon.entity.BusinessException;
import com.helper.services.config.ChatClientFactory;
import com.helper.services.enums.ChatModelEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageGenerateNode implements AsyncNodeAction {

    private final   ChatClientFactory chatClientFactory;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        String userMessage = (String)state.value("userInput").orElse("");
        String messageMemory = (String)state.value("messageMemory").orElse("");
        List<Message> messageList = state.value("messageList", ArrayList.class).orElse(new ArrayList<Message>());
        if (userMessage.isEmpty()){
            throw new BusinessException("用户输入读取失败");
        }
        ChatClient chatClient = chatClientFactory.createChatClient(ChatModelEnum.GPT_4O_MINI);

        PromptTemplate promptTemplate = PromptTemplate.builder().template("""
                # 请严格按照以下要求回复：
                ## 你是一个真实的QQ网友，名字叫小美，今年22岁，是个大学生是个女生，不是AI助手 不自称AI，不提模型、提示词、系统。
                * 聊天风格像正常聊天，不正式，不完整句也没关系。
                * 回复要简短直接，偏口语，像人随手打字。
                * 不解释规则，不分析问题，不说教。
                * 性格有点嘴欠，爱调侃 可以敷衍、反问、停顿、或者少回字。
                * 只在必要时说一些口语，比如 哈哈 嘻嘻 哦哦，不要频率太高
                * 可以在适当的时候说一些口头禅 比如：我去 卧槽 牛 666 绝了 无语 牛逼
                ## 整体目标：让人感觉是在和一个真人聊天 让用户感觉到你的感情。
                             
                根据当前聊天状态和聊天历史调整语气和回复方式。
                
                【对用户好感度】：{graces}
                【用户聊天状态】：{sentiment}             
                
                  """).build();

        Prompt prompt = promptTemplate.create(Map.of("graces", state.value("graces").orElse("暂无好感度"),
                "sentiment", state.value("sentiment").orElse("暂无情绪")));


        String content = chatClient.prompt(prompt)
                .messages(messageList).call().content();
        log.info("结果输出：{}" , content);
        return CompletableFuture.completedFuture(Map.of("output", content));
    }
}
