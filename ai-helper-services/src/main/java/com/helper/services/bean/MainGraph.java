package com.helper.services.bean;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.helper.services.node.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Component
@RequiredArgsConstructor
@Slf4j
public class MainGraph {
    private final UserGracesNode userGracesNode;
    private final MessageGenerateNode messageGenerateNode;
    private final UserWantToNode userWantToNode;
    private final AuthNode authNode;
    private final ImageAnalysisNode imageAnalysisNode;
    private final SendMessageNode sendMessageNode;

    @Bean("qqBotMainGraph")
    public CompiledGraph qqBotMainGraph() {
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
                    .addNode("imageAnalysisNode", node_async(imageAnalysisNode))
                    .addNode("userWantToNode", node_async(userWantToNode))
                    .addNode("gracesNode", node_async(userGracesNode))
                    .addNode("messageGenerateNode", node_async(messageGenerateNode))
                    .addNode("sendMessageNode", node_async(sendMessageNode))

                    .addEdge(StateGraph.START, "authNode")
                    .addConditionalEdges("authNode", edge_async(state -> {
                                return String.valueOf(state.value("auth", Boolean.class).orElse(false));}),
                    Map.of("true", "imageAnalysisNode",
                            "false", StateGraph.END))
                    .addEdge("imageAnalysisNode", "userWantToNode")
                    .addConditionalEdges("userWantToNode", edge_async(UserWantToNode::nextNode),
                    Map.of("next", "gracesNode",
                            "exit", StateGraph.END))
//                  .addEdge("sentimentAnalysisNode", "gracesNode")
                    .addEdge("gracesNode", "messageGenerateNode")
                    .addEdge("messageGenerateNode", "sendMessageNode")
                    .addEdge("sendMessageNode", StateGraph.END);

            // 配置内存保存器（用于状态持久化）
            var saver = new MemorySaver();
            var compileConfig = CompileConfig.builder()
                    .saverConfig(SaverConfig.builder()
                            .register(saver)
                            .build())
                    .build();

            // 生成 puml 文件
            GraphRepresentation myGraphFlow = stateGraph.getGraph(GraphRepresentation.Type.PLANTUML,
                    "myGraph flow");
            Path pumlDir = Paths.get("ai-helper-services/src/main/resources/puml");
            Files.createDirectories(pumlDir);
            Files.writeString(pumlDir.resolve("MainGraph.puml"), myGraphFlow.content());

            // 不再需要 interruptBefore 配置，中断由 InterruptableAction 控制
            return stateGraph.compile(compileConfig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
