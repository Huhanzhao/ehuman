package com.anyfriend.core.bean;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.anyfriend.core.node.*;
import com.anyfriend.core.utils.SendMessageUtil;
import com.anyfriend.dao.enums.LlmNodeTypeEnum;
import com.anyfriend.dao.enums.NodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Component
@RequiredArgsConstructor
@Slf4j
public class SendMessageCompileGraph {
    private final MessageGenerateNode messageGenerateNode;
    private final UserWantToNode userWantToNode;
    private final BotStatusSearchNode botStatusSearchNode;
    private final CheckAggregateNode checkAggregateNode;
    private final SendMessageUtil sendMessageUtil;

    private final ChatClientFactory chatClientFactory;


    @Bean("sendMessageCompiledGraph")
    public CompiledGraph getSendMessageGraph() {
        // 配置 KeyStrategyFactory
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
            keyStrategyHashMap.put("messages", new ReplaceStrategy());
            keyStrategyHashMap.put("failReason", new AppendStrategy());
            keyStrategyHashMap.put("human_feedback", new ReplaceStrategy());
            return keyStrategyHashMap;
        };

        StateGraph stateGraph = new StateGraph(keyStrategyFactory);

        try {
            stateGraph
                    .addNode("userWantToNode", node_async(userWantToNode))
                    .addNode("messageGenerateNode", node_async(messageGenerateNode))
                    .addNode("formatOutPutLlmNode", node_async(getLlmNode(LlmNodeTypeEnum.FORMAT_OUTPUT, NodeEnum.WANT_TO)))
                    .addNode("botStatusSearchNode", node_async(botStatusSearchNode))
                    .addNode("memoryVectorSearchNode", node_async(state -> Map.of()))  // todo 待完善
                    .addNode("graphSearchNode", node_async(state -> Map.of()))  // todo 待完善
                    .addNode("roleReversalCheckLlmNode", node_async(getLlmNode(LlmNodeTypeEnum.ROLE_REVERSAL_CHECK,NodeEnum.WANT_TO)))
                    .addNode("memoryCheckLlmNode", node_async(getLlmNode(LlmNodeTypeEnum.MEMORY_CHECK,NodeEnum.WANT_TO)))
                    .addNode("checkAggregateNode", node_async(checkAggregateNode))
                    .addNode("interactionCheckLlmNode", node_async(getLlmNode(LlmNodeTypeEnum.INTERACTION_CHECK,NodeEnum.WANT_TO)))
                    .addNode("dispatchToChecks", node_async(state -> Map.of())) // 用来充当并发中介
                    .addNode("sendMessageNode", node_async(state -> {
                        sendMessageUtil.sendChatMsg(
                                state.value("userId",Long.class).get(),
                                (String) state.value("formatOutPut").orElse("")
                        );
                        return Map.of();
                    }
                    ))

                    .addEdge(StateGraph.START, "userWantToNode")
                    .addEdge("userWantToNode", "botStatusSearchNode")
                    .addEdge("userWantToNode", "memoryVectorSearchNode")
                    .addEdge("userWantToNode", "graphSearchNode")
                    .addEdge("botStatusSearchNode", "messageGenerateNode")
                    .addEdge("memoryVectorSearchNode", "messageGenerateNode")
                    .addEdge("graphSearchNode", "messageGenerateNode")

                    // 审查
                    .addConditionalEdges("messageGenerateNode", state ->{
                        String generateMessage = (String) state.value("generateMessage").orElse("");

                        if (generateMessage.startsWith("<silence>") && generateMessage.endsWith("</silence>")){
                            sendMessageUtil.sendMegToSelf(generateMessage);
                            return CompletableFuture.completedFuture("end");
                        }
                        return CompletableFuture.completedFuture("next");
                    }, Map.of("end", StateGraph.END,"next","dispatchToChecks"))

                    .addEdge("dispatchToChecks", "memoryCheckLlmNode")
                    .addEdge("dispatchToChecks", "roleReversalCheckLlmNode")
                    .addEdge("dispatchToChecks", "interactionCheckLlmNode")
                    .addEdge("roleReversalCheckLlmNode", "checkAggregateNode")
                    .addEdge("memoryCheckLlmNode", "checkAggregateNode")
                    .addEdge("interactionCheckLlmNode", "checkAggregateNode")

                    .addConditionalEdges("checkAggregateNode", edge_async(CheckAggregateNode::nextNode),
                            Map.of("messageGenerateNode", "messageGenerateNode",
                                    "transformTagMessageNode", "formatOutPutLlmNode"))
                    .addEdge("formatOutPutLlmNode", "sendMessageNode")
                    .addEdge("sendMessageNode", StateGraph.END);

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
            Path pumlDir = Paths.get("anyfriend-core/src/main/resources/puml");
            Files.createDirectories(pumlDir);
            Files.writeString(pumlDir.resolve("sendMessage.puml"), sendMessageFlow.content());

            // 不再需要 interruptBefore 配置，中断由 InterruptableAction 控制
            return stateGraph.compile(compileConfig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public LlmNode getLlmNode(LlmNodeTypeEnum nodeTypeEnum, NodeEnum nodeEnum) {
        PromptTemplate promptTemplate = null;
        try {
            promptTemplate = PromptTemplate.builder().template(
                    Files.readString(Paths.get(nodeTypeEnum.getPromptTemplatePath()))).build();
            return LlmNode.builder()
                    .nodeName(nodeTypeEnum.getNodeName())
                    .promptTemplate(promptTemplate)
                    .outPutKey(nodeTypeEnum.getOutputKey())
                    .nodeEnum(nodeEnum)
                    .chatClientFactory(chatClientFactory)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
