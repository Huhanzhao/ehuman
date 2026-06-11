package com.bot.service.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.bot.service.bean.ChatClientFactory;
import com.bot.service.enums.NodeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.Map;

/**
 * @Author: huhanzhao
 * @CreateTime: 2026-06-11
 * @Description: 公用llmNode
 */

@Slf4j
@AllArgsConstructor
@Builder
public class LlmNode implements NodeAction {

    private String nodeName;

    private PromptTemplate promptTemplate;

    private String outPutKey;

    private NodeEnum nodeEnum;

    private ChatClientFactory chatClientFactory;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Map<String, Object> data = state.data();
        String prompt = this.promptTemplate.render(data);

        log.info("llmNode: {},prompt:{}",this.nodeName, prompt);

        ChatClient chatClient = chatClientFactory.createChatClient((Long) state.value("userId").get(), nodeEnum);

        String content = chatClient.prompt(prompt).call().content();
        log.info("llmNode: {},outPut:{}",this.nodeName, content);
        return Map.of(this.outPutKey, content);
    }
}
