package com.helper.services.bean;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.helper.services.node.system.*;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
@AllArgsConstructor
public class SystemGraph {
    private final ModelNodeNumNode modelNodeNumNode;
    private final ModelNameNode modelNameNode;
    private final ModelCategoryNode modelCategoryNode;
    private final ModelBaseUrlNode modelBaseUrlNode;
    private final ModelApiKeyNode modelApiKeyNode;

    public CompiledGraph getApiConfigGraph() {
        // 配置 KeyStrategyFactory 不配置会导致人类反馈拿不到状态
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
            keyStrategyHashMap.put("modelConfig", new ReplaceStrategy());
            keyStrategyHashMap.put("userInput", new ReplaceStrategy());
            keyStrategyHashMap.put("modelConfigSetStatus", new AppendStrategy());
            keyStrategyHashMap.put("userId", new ReplaceStrategy());
            return keyStrategyHashMap;
        };

        StateGraph stateGraph = new StateGraph(keyStrategyFactory);

        try {
            stateGraph
                    .addNode("modelCategoryNode", modelCategoryNode)
                    .addNode("modelNodeNumNode", modelNodeNumNode)
                    .addNode("modelNameNode", modelNameNode)
                    .addNode("modelBaseUrlNode", modelBaseUrlNode)
                    .addNode("modelApiKeyNode", modelApiKeyNode)

                    .addEdge( StateGraph.START, "modelCategoryNode")
                    .addEdge( "modelCategoryNode", "modelNodeNumNode")
                    .addEdge( "modelNodeNumNode", "modelNameNode")
                    .addEdge( "modelNameNode", "modelBaseUrlNode")
                    .addEdge( "modelBaseUrlNode", "modelApiKeyNode")
                    .addEdge("modelApiKeyNode", StateGraph.END);

            // 配置内存保存器（用于状态持久化）
            var saver = new MemorySaver();
            var compileConfig = CompileConfig.builder()
                    .saverConfig(SaverConfig.builder()
                            .register(saver)
                            .build())
                    .build();
            // 不再需要 interruptBefore 配置，中断由 InterruptableAction 控制
            return stateGraph.compile(compileConfig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CompiledGraph getVoiceGraph() {
        // todo 配置需要克隆的参考音频保存到数据库

        // 配置 KeyStrategyFactory 不配置会导致人类反馈拿不到状态
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
            keyStrategyHashMap.put("modelConfig", new ReplaceStrategy());
            keyStrategyHashMap.put("userInput", new ReplaceStrategy());
            keyStrategyHashMap.put("modelConfigSetStatus", new AppendStrategy());
            keyStrategyHashMap.put("userId", new ReplaceStrategy());
            return keyStrategyHashMap;
        };

        StateGraph stateGraph = new StateGraph(keyStrategyFactory);

        try {
            stateGraph
                    .addNode("modelCategoryNode", modelCategoryNode)
                    .addNode("modelNodeNumNode", modelNodeNumNode)
                    .addNode("modelNameNode", modelNameNode)
                    .addNode("modelBaseUrlNode", modelBaseUrlNode)
                    .addNode("modelApiKeyNode", modelApiKeyNode)

                    .addEdge( StateGraph.START, "modelCategoryNode")
                    .addEdge( "modelCategoryNode", "modelNodeNumNode")
                    .addEdge( "modelNodeNumNode", "modelNameNode")
                    .addEdge( "modelNameNode", "modelBaseUrlNode")
                    .addEdge( "modelBaseUrlNode", "modelApiKeyNode")
                    .addEdge("modelApiKeyNode", StateGraph.END);

            // 配置内存保存器（用于状态持久化）
            var saver = new MemorySaver();
            var compileConfig = CompileConfig.builder()
                    .saverConfig(SaverConfig.builder()
                            .register(saver)
                            .build())
                    .build();
            // 不再需要 interruptBefore 配置，中断由 InterruptableAction 控制
            return stateGraph.compile(compileConfig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
