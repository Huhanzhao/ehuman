package com.anyfriend.core.bean;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.anyfriend.core.config.NodeStatePersistenceListener;
import com.anyfriend.core.node.BotStatusSearchNode;
import com.anyfriend.core.node.CheckAggregateNode;
import com.anyfriend.core.node.LlmNode;
import com.anyfriend.core.node.UserWantToNode;
import com.anyfriend.core.service.AfterLlmCallback;
import com.anyfriend.core.service.BeforeLlmCallback;
import com.anyfriend.core.utils.MemoryCastUtils;
import com.anyfriend.core.utils.SendMessageUtil;
import com.anyfriend.dao.enums.LlmNodeTypeEnum;
import com.anyfriend.dao.enums.NodeEnum;
import com.anyfriend.dao.service.TbAiChatMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Component
@RequiredArgsConstructor
@Slf4j
public class SendMessageCompileGraph {
    private final UserWantToNode userWantToNode;
    private final BotStatusSearchNode botStatusSearchNode;
    private final CheckAggregateNode checkAggregateNode;
    private final SendMessageUtil sendMessageUtil;
    private final TbAiChatMemoryService tbAiChatMemoryService;

    private final ChatClientFactory chatClientFactory;
    private final NodeStatePersistenceListener nodeStatePersistenceListener;


    @Bean("sendMessageCompiledGraph")
    public CompiledGraph getSendMessageGraph() {
        // 配置 KeyStrategyFactory
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
            keyStrategyHashMap.put("messages", new ReplaceStrategy());
            keyStrategyHashMap.put("userInfo", new ReplaceStrategy());
            keyStrategyHashMap.put("failReason", new AppendStrategy());
            keyStrategyHashMap.put("human_feedback", new ReplaceStrategy());
            return keyStrategyHashMap;
        };

        StateGraph stateGraph = new StateGraph(keyStrategyFactory);

        try {
            // 创建消息生成节点
            LlmNode messageGenerate = getLlmNode(LlmNodeTypeEnum.MESSAGE_GENERATE,
                    NodeEnum.MASTER,
                    state -> {
                        Long userId = (Long)state.value("userId").orElse(0L);
                        Boolean initiative = (Boolean)state.value("initiative").orElse(false);
                        List<Message> messageList = null;
                        if (!initiative){
                            messageList = MemoryCastUtils.memoryCastToMessages(
                                    tbAiChatMemoryService.getMessageList(userId, 15));
                        }else{
                            messageList = MemoryCastUtils.memoryCastToInitiativeMessages(
                                    tbAiChatMemoryService.getMessageList(userId, 15));
                        }
                        return new BeforeLlmCallback.BeforeCallBackEntity(messageList, null,null);
                    },
                    null);

            LlmNode combinedCheckNode = getLlmNode(LlmNodeTypeEnum.COMBINED_CHECK, NodeEnum.AUXILIARY, null, null);

            stateGraph
                    .addNode("userWantToNode", node_async(userWantToNode))
                    .addNode("messageGenerateNode", node_async(messageGenerate))
                    .addNode("formatOutPutLlmNode", node_async(getLlmNode(LlmNodeTypeEnum.FORMAT_OUTPUT, NodeEnum.AUXILIARY,null,null)))
                    .addNode("botStatusSearchNode", botStatusSearchNode)
                    .addNode("memoryVectorSearchNode", node_async(state -> Map.of()))  // todo 待完善
                    .addNode("graphSearchNode", node_async(state -> Map.of()))  // todo 待完善
                    .addNode("combinedCheckNode", node_async(combinedCheckNode))
                    .addNode("checkAggregateNode", node_async(checkAggregateNode))
                    .addNode("sendMessageNode", node_async(state -> {
                        String message = state.value("formatOutPut",String.class).orElse("");
                        if (message.isEmpty()){
                            message = state.value("generateMessage",String.class).orElse("");
                        }
                        sendMessageUtil.sendChatMsg(
                                state.value("userId",Long.class).get(),
                                message
                        );
                        CompletableFuture.runAsync(() -> {
                            // todo 判断下次发送消息
//                            String result = callSlowApi(input);
                        });
                        return Map.of();
                    }))

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
                    }, Map.of("end", StateGraph.END,"next","combinedCheckNode"))

                    .addEdge("combinedCheckNode", "checkAggregateNode")

                    .addConditionalEdges("checkAggregateNode", edge_async(checkAggregateNode::nextNode),
                            Map.of("fail", "messageGenerateNode",
                                    "format","formatOutPutLlmNode",
                                    "pass", "sendMessageNode"))
                    .addEdge("formatOutPutLlmNode", "sendMessageNode")
                    .addEdge("sendMessageNode", StateGraph.END);

            // 配置内存保存器（用于状态持久化）
            var saver = new MemorySaver();
            var compileConfig = CompileConfig.builder()
                    .withLifecycleListener(nodeStatePersistenceListener)
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

    public LlmNode getLlmNode(LlmNodeTypeEnum nodeTypeEnum, NodeEnum nodeEnum, BeforeLlmCallback beforeLlmCallback, AfterLlmCallback afterLlmCallback) {
        PromptTemplate promptTemplate = null;
        try {
            promptTemplate = PromptTemplate.builder().template(
                    Files.readString(Paths.get(nodeTypeEnum.getPromptTemplatePath()))).build();
            LlmNode.LlmNodeBuilder llmNodeBuilder = LlmNode.builder()
                    .nodeName(nodeTypeEnum.getNodeName())
                    .promptTemplate(promptTemplate)
                    .outPutKey(nodeTypeEnum.getOutputKey())
                    .beforeLlmCallback(beforeLlmCallback)
                    .afterLlmCallback(afterLlmCallback)
                    .nodeEnum(nodeEnum)
                    .chatClientFactory(chatClientFactory);
            return llmNodeBuilder.build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
