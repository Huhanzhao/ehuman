package com.anyfriend.core.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.anyfriend.core.bean.ChatClientFactory;
import com.anyfriend.core.utils.MemoryCastUtils;
import com.anyfriend.dao.enums.NodeEnum;
import com.anyfriend.dao.service.TbAiChatMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Component
public class MessageGenerateNode implements NodeAction {

    // todo 改造成LlmNode 形式 人设提示词重新调整

    private final ChatClientFactory chatClientFactory;

    private final TbAiChatMemoryService tbAiChatMemoryService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        Long userId = (Long)state.value("userId").orElse(0L);
        List<Message> messageList = MemoryCastUtils.memoryCastToMessages(
                tbAiChatMemoryService.getMessageList(userId, 15));
        ChatClient chatClient = chatClientFactory.createChatClient(userId, NodeEnum.GENERATE);

        String template = null;
        try {
            template = Files.readString(Paths.get("./md/MainPrompt.md"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        PromptTemplate promptTemplate = PromptTemplate.builder().template(template).build();

        // 获取提示词变量
        String prompt = promptTemplate.render(state.data());

        log.info("提示词：{}" , prompt);
        ChatClient.ChatClientRequestSpec chatClientRequestSpec = chatClient.prompt(prompt)
                .messages(messageList);

        Boolean initiative = (Boolean)state.value("initiative").orElse(false);
        String content = null;
        if (initiative){
            content = chatClientRequestSpec.user("这条消息是你主动给用户发送的消息，不是用户主动给你发送的消息").call().content();
        }else{
            content = chatClientRequestSpec.call().content();
        }
        log.info("结果输出：{}" , content);
        return Map.of("generateMessage", content);
    }
}
