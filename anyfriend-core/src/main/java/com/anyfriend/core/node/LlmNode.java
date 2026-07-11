package com.anyfriend.core.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.anyfriend.core.bean.ChatClientFactory;
import com.anyfriend.core.service.AfterLlmCallback;
import com.anyfriend.core.service.BeforeLlmCallback;
import com.anyfriend.dao.enums.LlmNodeTypeEnum;
import com.anyfriend.dao.enums.NodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    private final NodeEnum nodeEnum;

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

    // 单节点调用
    public String invoke(Long userId,Map<String, Object> promptParams){
        // 调用大模型
        String prompt = fillPromptTemplate(promptParams, this.promptTemplate);
        log.info("单节点调用 llmNodeName: {},prompt:{}",this.nodeName, prompt);

        ChatClient chatClient = chatClientFactory.createChatClient(userId, nodeEnum);

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


    public LlmNode(LlmNodeTypeEnum nodeTypeEnum,
                              NodeEnum nodeEnum,
                              BeforeLlmCallback beforeLlmCallback,
                              AfterLlmCallback afterLlmCallback,
                              ChatClientFactory chatClientFactory
    ) {
        try {
            this.promptTemplate = PromptTemplate.builder().template(
                    Files.readString(Paths.get(nodeTypeEnum.getPromptTemplatePath()))).build();
            this.nodeName = nodeTypeEnum.getNodeName();
            this.outPutKey = nodeTypeEnum.getOutputKey();
            this.beforeLlmCallback = beforeLlmCallback;
            this.afterLlmCallback = afterLlmCallback;
            this.nodeEnum = nodeEnum;
            this.chatClientFactory = chatClientFactory;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public LlmNode(LlmNodeTypeEnum nodeTypeEnum,
                   NodeEnum nodeEnum,
                   ChatClientFactory chatClientFactory){
        this(nodeTypeEnum,nodeEnum,null,null,chatClientFactory);
    }
}