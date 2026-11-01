package com.ehuman.core.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.ehuman.core.bean.ChatClientFactory;
import com.ehuman.core.service.CommonAuxiliaryService;
import com.ehuman.core.utils.MemoryCastUtils;
import com.ehuman.core.utils.PromptReadUtil;
import com.ehuman.dao.entity.TbAiChatMemoryDo;
import com.ehuman.dao.enums.LlmTypeEnum;
import com.ehuman.dao.service.TbAiChatMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.ehuman.dao.enums.LlmChatEnum.USER_WANT_TO;

@RequiredArgsConstructor
@Slf4j
@Component
public class UserWantToNode implements NodeAction {
    private final ChatClientFactory chatClientFactory;

    private final TbAiChatMemoryService tbAiChatMemoryService;

    private final CommonAuxiliaryService commonAuxiliaryService;

    @Value("${bot.prompt-path}")
    private String promptPathPrefix;

    @Override
    public Map<String, Object> apply (OverAllState state) throws IOException {
        Long userId = (Long) state.value("userId").orElse(0L);
        Boolean initiative = (Boolean)state.value("initiative").orElse(false);
        // 查询历史消息
        List<TbAiChatMemoryDo> messageList = tbAiChatMemoryService.getMessageList(userId, 15);
        String memoryString = MemoryCastUtils.memoryCastToString(messageList,true);
        // 异步调用流水账记录
        CompletableFuture.runAsync(() -> commonAuxiliaryService.summaryMessages(userId,20))
                .exceptionally(throwable -> {
                    log.error("summaryMessages 异步执行失败，userId: {}", userId, throwable);
                    throwable.printStackTrace();
                    return null;
                });
        if (initiative){
            // 主动发送消息跳过
            return Map.of("userWantTo", "next","memoryString", memoryString);
        }
        ChatClient chatClient = chatClientFactory.createChatClient(userId, LlmTypeEnum.AUXILIARY);
        PromptTemplate promptTemplate = PromptReadUtil.readPrompt(USER_WANT_TO, promptPathPrefix);

        String prompt = promptTemplate.render(Map.of("memoryString", memoryString));
        String content = chatClient.prompt(prompt)
                .user(" 以上是用户与ai对话历史，请分析会话是否结束，是否需要ai回复").call().content();
        log.info("用户意图结果：{}", content);
        return Map.of("userWantTo", content,"memoryString", memoryString);
    }

    public static String nextNode(OverAllState state) {
        // 路由下一个节点
        return (String) state.value("userWantTo").orElse("");
    }
}
