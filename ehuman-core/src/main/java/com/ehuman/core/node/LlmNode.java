package com.ehuman.core.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.ehuman.core.bean.ChatClientFactory;
import com.ehuman.core.service.AfterLlmCallback;
import com.ehuman.core.service.BeforeLlmCallback;
import com.ehuman.core.utils.PromptReadUtil;
import com.ehuman.dao.enums.LlmChatEnum;
import com.ehuman.dao.enums.LlmTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author: huhanzhao
 * @CreateTime: 2026-06-11
 * @Description: 公用llmNode
 */

@Slf4j
public class LlmNode implements NodeAction {

    private final String nodeName;

    private final PromptTemplate promptTemplate;

    private final String outPutKey;

    private final LlmTypeEnum llmTypeEnum;

    private final ChatClientFactory chatClientFactory;

    private final BeforeLlmCallback beforeLlmCallback;

    private final AfterLlmCallback afterLlmCallback;

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

        ChatClient chatClient = chatClientFactory.createChatClient((Long) state.value("userId").get(), llmTypeEnum);

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

    // 单节点调用
    public String invoke(Long userId,Map<String, Object> promptParams){
        // 调用大模型
        String prompt = fillPromptTemplate(promptParams, this.promptTemplate);
        log.info("单节点调用 llmNodeName: {},prompt:{}",this.nodeName, prompt);

        ChatClient chatClient = chatClientFactory.createChatClient(userId, llmTypeEnum);

        String content = chatClient.prompt(prompt).call().content();
        log.info("单节点调用 llmNode: {},outPut:{}",this.nodeName, content);

        return content;
    }

    private String fillPromptTemplate(Map<String,Object> promptParams, PromptTemplate promptTemplate){
        AtomicReference<String> template = new AtomicReference<>(promptTemplate.getTemplate());
        promptParams.forEach((key, value) -> {
            if (template.get().contains("{" + key + "}")){
                template.set(template.get().replace("{" + key + "}", value.toString()));
            }
        });
        return template.get();
    }


    public LlmNode(LlmChatEnum nodeTypeEnum,
                   LlmTypeEnum llmTypeEnum,
                   BeforeLlmCallback beforeLlmCallback,
                   AfterLlmCallback afterLlmCallback,
                   ChatClientFactory chatClientFactory
    ) {
        this.promptTemplate = PromptReadUtil.readPrompt(nodeTypeEnum);
        this.nodeName = nodeTypeEnum.getNodeName();
        this.outPutKey = nodeTypeEnum.getOutputKey();
        this.beforeLlmCallback = beforeLlmCallback;
        this.afterLlmCallback = afterLlmCallback;
        this.llmTypeEnum = llmTypeEnum;
        this.chatClientFactory = chatClientFactory;
    }

    public LlmNode(LlmChatEnum nodeTypeEnum,
                   LlmTypeEnum llmTypeEnum,
                   ChatClientFactory chatClientFactory){
        this(nodeTypeEnum, llmTypeEnum,null,null,chatClientFactory);
    }
}