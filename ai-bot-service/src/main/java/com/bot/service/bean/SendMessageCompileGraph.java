package com.bot.service.bean;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.bot.service.node.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Component
@RequiredArgsConstructor
@Slf4j
public class SendMessageCompileGraph {
    private final MessageGenerateNode messageGenerateNode;
    private final UserWantToNode userWantToNode;
    private final BotStatusSearchNode botStatusSearchNode;
    private final MemoryVectorSearchNode memoryVectorSearchNode;
    private final GraphSearchNode graphSearchNode;

    @Bean("sendMessageCompiledGraph")
    public CompiledGraph getSendMessageGraph() {
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
                    .addNode("userWantToNode", node_async(userWantToNode))
                    .addNode("messageGenerateNode", node_async(messageGenerateNode))
                    .addNode("botStatusSearchNode", node_async(botStatusSearchNode))
                    .addNode("memoryVectorSearchNode", node_async(memoryVectorSearchNode))
                    .addNode("graphSearchNode", node_async(graphSearchNode))

                    .addEdge(StateGraph.START, "userWantToNode")
                    .addEdge("userWantToNode", "botStatusSearchNode")
                    .addEdge("userWantToNode", "memoryVectorSearchNode")
                    .addEdge("userWantToNode", "graphSearchNode")
                    .addEdge("botStatusSearchNode", "messageGenerateNode")
                    .addEdge("memoryVectorSearchNode", "messageGenerateNode")
                    .addEdge("graphSearchNode", "messageGenerateNode")
                    // todo bot状态节点？ 异步修改心情
                    .addEdge("messageGenerateNode", StateGraph.END);

            // 配置内存保存器（用于状态持久化）
            var saver = new MemorySaver();
            var compileConfig = CompileConfig.builder()
                    .saverConfig(SaverConfig.builder()
                            .register(saver)
                            .build())
                    .build();

            // 生成 puml 文件
            GraphRepresentation sendMessageFlow = stateGraph.getGraph(GraphRepresentation.Type.PLANTUML,
                    "sendMessage flow");
            Path pumlDir = Paths.get("ai-bot-service/src/main/resources/puml");
            Files.createDirectories(pumlDir);
            Files.writeString(pumlDir.resolve("MainGraph.puml"), sendMessageFlow.content());

            // 不再需要 interruptBefore 配置，中断由 InterruptableAction 控制
            return stateGraph.compile(compileConfig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
