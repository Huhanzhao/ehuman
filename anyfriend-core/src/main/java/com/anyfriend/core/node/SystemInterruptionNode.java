package com.anyfriend.core.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.anyfriend.core.service.AfterLlmCallback;
import com.anyfriend.core.utils.SendMessageUtil;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Builder
@Data
public class SystemInterruptionNode implements AsyncNodeActionWithConfig, InterruptableAction {

    private SendMessageUtil sendMessageUtil;

    private String nodeName;

    private String interruptKey;  // 根据哪个key进行中断

    private String inputPrompt;   // 中断提醒用户输入的提示语

    private Map<String, Object> inputMapping;  // 用户输入序号与值映射

    private AfterLlmCallback afterLlmCallback;  // 后续执行

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        String userInput = state.value("userInput",String.class).orElse("").trim();
        // 正常节点逻辑：更新状态
        if (inputMapping != null && inputMapping.containsKey(userInput)){
            state.updateState(Map.of(interruptKey, inputMapping.get(userInput)));
        }else {
            state.updateState(Map.of(interruptKey, userInput));
        }
        state.updateState(Map.of("userInput", ""));
        // 执行后续逻辑
        if (afterLlmCallback != null) afterLlmCallback.execute(state, "");
        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
        log.info("进入了{}节点中断方法",nodeName);
        // 检查是否需要中断
        String userInput = state.value("userInput",String.class).orElse("").trim();
        Long userId = Long.valueOf(state.value("userId", String.class).orElse(""));
        InterruptionMetadata interruption = null;
        if (userInput.isEmpty()) {
            // 中断等待用户输入
            sendMessageUtil.sendSystemMsg(userId, inputPrompt);
            // 返回 InterruptionMetadata 来中断执行
            interruption = InterruptionMetadata.builder(nodeId, state)
                    .addMetadata("message", nodeName + "节点中断")
                    .addMetadata("node", nodeId)
                    .build();
            log.info("{}节点中断",nodeName);
            return Optional.of(interruption);
        }

        // 判断用户输入是否合法
        if (inputMapping != null && !inputMapping.containsKey(userInput)){
            sendMessageUtil.sendSystemMsg(userId, "请按照要求输入");
            // 返回 InterruptionMetadata 来中断执行
            interruption = InterruptionMetadata.builder(nodeId, state)
                    .addMetadata("message", nodeName + "节点中断")
                    .addMetadata("node", nodeId)
                    .build();
            return Optional.of(interruption);
        }

        return Optional.empty();
    }
}
