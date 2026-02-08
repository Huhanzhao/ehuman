package com.helper.services.node;

import ch.qos.logback.classic.Logger;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class IntrerrptableNodeTest implements AsyncNodeActionWithConfig, InterruptableAction {
    @Override
    public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
        // 检查是否需要中断
        // 如果状态中没有 human_feedback，则中断等待用户输入
        Optional<Object> humanFeedback = state.value("human_feedback");

        if (humanFeedback.isEmpty()) {
            // 返回 InterruptionMetadata 来中断执行
            InterruptionMetadata interruption = InterruptionMetadata.builder(nodeId, state)
                    .addMetadata("message", "等待用户输入...")
                    .addMetadata("node", nodeId)
                    .build();
            log.info("等待用户输入");
            return Optional.of(interruption);
        }

        // 如果已经有 human_feedback，继续执行
        return Optional.empty();
    }

    @Override  // 正常执行
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        // 正常节点逻辑：更新状态
        log.info("正常执行逻辑");
        return CompletableFuture.completedFuture(Map.of("messages", "正常执行了节点"));
    }
}
