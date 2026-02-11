package com.helper.services.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.helper.commonservicescommon.entity.BusinessException;
import com.helper.services.bean.ChatClientFactory;
import com.helper.services.enums.ChatModelEnum;
import com.helper.services.enums.NodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Slf4j
@Component
public class MessageCheckNode implements NodeAction {

    private final ChatClientFactory chatClientFactory;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String aiMessage = (String) state.value("output", String.class).orElse("");
        Long userId = (Long)state.value("userId").orElse(0L);
        List<Message> messageList = state.value("messageList", ArrayList.class).orElse(new ArrayList<Message>());
        if (aiMessage.isEmpty()){
            throw new BusinessException("ai输出读取失败");
        }
        ChatClient chatClient = chatClientFactory.createChatClient(userId, NodeEnum.GRACE);

        String prompt = """
                # 你是一个专业ai消息回复审核者，你需要判断ai的输出是否合格，在原输出消息的基础上进行消息更改。
                ## 审核要求：
                * 1.消息在不破坏语义的情况下用 ”&&“ 拆分成多条。
                * 2.当只有简短的几个字的时候可以不做处理 例如：加油！ 例如：拜拜 例如：好的。
                
                例1：ai: 好的好的 单词 "impact" 是影响的意思 下一个单词 "justify" 怎么翻译？
                需要把ai的消息改成：好的好的&&单词 "impact" 是影响的意思&&下一个单词 "justify" 怎么翻译
                （分隔了消息，正常保留问句）
                
                例2：ai: 我啊 就是个随便聊聊的人 不分性别 你觉得呢？
                需要把ai的消息改成：我啊&&就是个随便聊聊的人&&不分性别
                (做了两件事：1.去掉了不必要的反问 2.把消息用&&分开)
                
                例2：ai: 想要聊些什么呢 你想要说些什么方面的呢 有什么可以帮你的吗 有什么想聊的吗
                这些都可以一眼看出来是ai的消息类型的内容，可以在不必要的时候去掉
                """;

        String content = chatClient.prompt(prompt)
                .user(aiMessage).call().content();
        log.info("messageCheck结果输出：{}" , content);
        return Map.of("finalOutput", content);
    }
}
