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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageCheckNode implements AsyncNodeAction {

    private final ChatClientFactory chatClientFactory;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        String aiMessage = (String) state.value("output", String.class).orElse("");
        List<Message> messageList = state.value("messageList", ArrayList.class).orElse(new ArrayList<Message>());
        if (aiMessage.isEmpty()){
            throw new BusinessException("ai输出读取失败");
        }
        ChatClient chatClient = chatClientFactory.createChatClient(ChatModelEnum.GPT_4O_MINI);

        String prompt = """
                # 你是一个专业ai消息回复审核者，你需要判断ai的输出是否合格，在原输出消息的基础上进行消息更改。
                ## 审核要求：
                * 1.消息中不能带有强行的问话，只在必要时询问用户。
                * 2.消息在不破坏语义的情况下用 ”&&“ 拆分成多条。
                * 3.语言太官方或者太像ai回复，就改一下原句，变得更像是人类语言。
                
                例1：user: 继续提问下一个单词
                     ai: 好的好的 单词 "impact" 是影响的意思 下一个单词 "justify" 怎么翻译？
                需要把ai的消息改成：好的好的&&单词 "impact" 是影响的意思&&下一个单词 "justify" 怎么翻译
                （分隔了消息，正常保留问句）
                
                例2：user: 你是男的女的
                     ai: 我啊 就是个随便聊聊的人 不分性别 你觉得呢？
                需要把ai的消息改成：我啊&&就是个随便聊聊的人&&不分性别
                (做了两件事：1.去掉了不必要的反问 2.把消息用&&分开)
                
                例2： 想要聊些什么呢 你想要说些什么方面的呢 有什么可以帮你的吗 有什么想聊的吗
                这些都可以一眼看出来是ai的消息类型的内容，可以在不必要的时候去掉，必要的时候改成口语化比如：有啥事儿吗 怎么了 等等，不一定要根据例子改，自由发挥即可
                目标是去除ai味儿
                
                只需要回复我修改之后的内容即可，不要回复多余的话
                """;

        String content = chatClient.prompt(prompt)
                .user(aiMessage).call().content();
        log.info("messageCheck结果输出：{}" , content);
        return CompletableFuture.completedFuture(Map.of("finalOutput", content));
    }
}
