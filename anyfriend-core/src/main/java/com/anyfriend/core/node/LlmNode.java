package com.anyfriend.core.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.anyfriend.core.bean.ChatClientFactory;
import com.anyfriend.core.service.AfterLlmCallback;
import com.anyfriend.core.service.BeforeLlmCallback;
import com.anyfriend.dao.enums.NodeEnum;
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

    private String userMessage;

    private ChatClientFactory chatClientFactory;

    private BeforeLlmCallback beforeLlmCallback;

    private AfterLlmCallback afterLlmCallback;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Map<String, Object> data = state.data();

        BeforeLlmCallback.BeforeCallBackEntity beforeCallBackEntity = null;
        // 前置逻辑
        if (beforeLlmCallback != null){
            beforeCallBackEntity = beforeLlmCallback.execute(state);
        }


        // 调用大模型
        String prompt = this.promptTemplate.render(data);

        log.info("llmNode: {},prompt:{}",this.nodeName, prompt);

        ChatClient chatClient = chatClientFactory.createChatClient((Long) state.value("userId").get(), nodeEnum);

        ChatClient.ChatClientRequestSpec chatClientRequestSpec = chatClient.prompt(prompt);
        if (beforeCallBackEntity != null && beforeCallBackEntity.userMessage() != null){
            chatClientRequestSpec.user(beforeCallBackEntity.userMessage());
        }
        if (beforeCallBackEntity != null && beforeCallBackEntity.messages() != null){
            chatClientRequestSpec.messages(beforeCallBackEntity.messages());
        }
        String content = chatClientRequestSpec.call().content();
        log.info("llmNode: {},outPut:{}",this.nodeName, content);


        // 后置逻辑
        if (afterLlmCallback != null){
            afterLlmCallback.execute(state, content);
        }

        return Map.of(this.outPutKey, content);
    }

}