package com.helper.services.bean;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.helper.services.node.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Component
@RequiredArgsConstructor
public class MainGraph {
    private final SentimentAnalysisNode sentimentAnalysisNode;
    private final MessageCheckNode messageCheckNode;
    private final UserGracesNode userGracesNode;
    private final MessageGenerateNode messageGenerateNode;
    private final UserWantToNode userWantToNode;
    private final AuthNode authNode;

    @Bean
    public CompiledGraph getQQBotCompiledGraph() {
        // 配置 KeyStrategyFactory
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
            keyStrategyHashMap.put("messages", new AppendStrategy());
            keyStrategyHashMap.put("human_feedback", new ReplaceStrategy());
            return keyStrategyHashMap;
        };

        StateGraph stateGraph = new StateGraph(keyStrategyFactory);

        try {
            stateGraph
                    .addNode("authNode", node_async(authNode))
                    .addNode("userWantToNode", node_async(userWantToNode))
//                    .addNode("sentimentAnalysisNode", node_async(sentimentAnalysisNode))
                    .addNode("gracesNode", node_async(userGracesNode))
                    .addNode("messageGenerateNode", node_async(messageGenerateNode))
//                    .addNode("messageCheckNode", node_async(messageCheckNode))

                    .addEdge(StateGraph.START, "authNode")
                    .addConditionalEdges("authNode", edge_async(state -> {
                                return String.valueOf(state.value("auth", Boolean.class).orElse(false));
                            }),
                    Map.of("true", "userWantToNode",
                            "false", StateGraph.END))

                    .addConditionalEdges("userWantToNode", edge_async(UserWantToNode::nextNode),
                    Map.of("next", "gracesNode",
                            "exit", StateGraph.END))

//                    .addEdge("sentimentAnalysisNode", "gracesNode")
                    .addEdge("gracesNode", "messageGenerateNode")
                    .addEdge("messageGenerateNode", StateGraph.END);

            // 配置内存保存器（用于状态持久化）
            var saver = new MemorySaver();
            var compileConfig = CompileConfig.builder()
                    .saverConfig(SaverConfig.builder()
                            .register(saver)
                            .build())
                    // 不再需要 interruptBefore 配置，中断由 InterruptableAction 控制
                    .build();

            return stateGraph.compile(compileConfig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
