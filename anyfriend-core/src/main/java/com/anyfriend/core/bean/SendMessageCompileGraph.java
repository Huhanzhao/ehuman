package com.anyfriend.core.bean;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.fastjson2.JSON;
import com.anyfriend.core.config.NodeStatePersistenceListener;
import com.anyfriend.core.job.JobManager;
import com.anyfriend.core.node.BotStatusSearchNode;
import com.anyfriend.core.node.CheckAggregateNode;
import com.anyfriend.core.node.LlmNode;
import com.anyfriend.core.node.UserWantToNode;
import com.anyfriend.core.service.CommonAuxiliaryService;
import com.anyfriend.core.utils.SendMessageUtil;
import com.anyfriend.dao.entity.TbAiChatMemoryDo;
import com.anyfriend.dao.enums.LlmNodeTypeEnum;
import com.anyfriend.dao.enums.NodeEnum;
import com.anyfriend.dao.service.TbAiChatMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
    private final CommonAuxiliaryService commonAuxiliaryService;

    private final ChatClientFactory chatClientFactory;
    private final NodeStatePersistenceListener nodeStatePersistenceListener;

    private final JobManager jobManager;


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
            LlmNode messageGenerate = new LlmNode(LlmNodeTypeEnum.MESSAGE_GENERATE,
                    NodeEnum.MASTER,
                    chatClientFactory);

            // 合并检查节点
            LlmNode combinedCheckNode = new LlmNode(LlmNodeTypeEnum.COMBINED_CHECK, NodeEnum.AUXILIARY, chatClientFactory);

            // 主动发消息生成节点
            LlmNode initiativeMessageGenerateNode = new LlmNode(LlmNodeTypeEnum.INITIATIVE_MESSAGE_GENERATE, NodeEnum.MASTER, chatClientFactory);

            stateGraph
                    .addNode("userWantToNode", node_async(userWantToNode))
                    .addNode("messageGenerateNode", node_async(messageGenerate))
                    .addNode("initiativeMessageGenerateNode", node_async(initiativeMessageGenerateNode))
                    .addNode("formatOutPutLlmNode", node_async(new LlmNode(LlmNodeTypeEnum.FORMAT_OUTPUT, NodeEnum.AUXILIARY,chatClientFactory)))
                    .addNode("botStatusSearchNode", botStatusSearchNode)
                    .addNode("memoryVectorSearchNode", node_async(state -> Map.of()))  // todo 待完善
                    .addNode("graphSearchNode", node_async(state -> Map.of()))  // todo 待完善
                    .addNode("initiativeOrNotNode", node_async(state -> Map.of()))  // 主动被动中转节点
                    .addNode("combinedCheckNode", node_async(combinedCheckNode))
                    .addNode("checkAggregateNode", node_async(checkAggregateNode))
                    .addNode("sendMessageNode", node_async(state -> {
                        String message = state.value("formatOutPut",String.class).orElse("");
                        Long userId = state.value("userId", Long.class).get();
                        if (message.isEmpty()){
                            message = state.value("generateMessage",String.class).orElse("");
                        }
                        sendMessageUtil.sendChatMsg(userId, message);

                        // 对话之后的异步工作
                        doAsyncAfterSendMessage(state);
                        return Map.of();
                    }))

                    .addEdge(StateGraph.START, "userWantToNode")
                    .addEdge("userWantToNode", "botStatusSearchNode")
                    .addEdge("userWantToNode", "memoryVectorSearchNode")
                    .addEdge("userWantToNode", "graphSearchNode")
                    .addEdge("botStatusSearchNode", "initiativeOrNotNode")
                    .addEdge("memoryVectorSearchNode", "initiativeOrNotNode")
                    .addEdge("graphSearchNode", "initiativeOrNotNode")
                    .addConditionalEdges("initiativeOrNotNode",
                            edge_async(state -> state.value("initiative",Boolean.class).orElse(false).toString()),
                            Map.of("true","initiativeMessageGenerateNode","false","messageGenerateNode"))

                    // 审查
                    .addEdge("initiativeMessageGenerateNode","combinedCheckNode")
                    .addConditionalEdges("messageGenerateNode", state ->{
                        String generateMessage = (String) state.value("generateMessage").orElse("");
                        Long userId = state.value("userId", Long.class).get();

                        if (generateMessage.startsWith("<silence>") && generateMessage.endsWith("</silence>")){
                            sendMessageUtil.sendMegToSelf(generateMessage);
                            tbAiChatMemoryService.save(new TbAiChatMemoryDo().setMemoryId(userId).setRole("assistant").setMessage(generateMessage));
                            return CompletableFuture.completedFuture("end");
                        }
                        return CompletableFuture.completedFuture("next");
                    }, Map.of("end", StateGraph.END,"next","combinedCheckNode"))

                    .addEdge("combinedCheckNode", "checkAggregateNode")

                    .addConditionalEdges("checkAggregateNode", edge_async(checkAggregateNode::nextNode),
                            Map.of("fail", "messageGenerateNode",
                                    "initiativeFail", "initiativeMessageGenerateNode",
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

    private void doAsyncAfterSendMessage(OverAllState state) {
        // 添加一个五分钟之后的定时任务（五分钟没有用户回复再去执行要不要主动发送消息的任务）
        CompletableFuture.runAsync(() -> {
            try {
                jobManager.deleteJobByStartString("callNextSendMessageJob_");

                // 当前时间 + 5分钟
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime future = now.plusMinutes(5);
                // 格式化
                String result = future.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                jobManager.addJob("callNextSendMessageJob_" + System.currentTimeMillis(),"system",
                        commonAuxiliaryService.toCronExpression(result),
                        "com.anyfriend.core.job.action.CallSendMessageJob",
                        JSON.toJSONString(state.data()));
            } catch (Exception e) {
                log.error("修改 callNextSendMessageJob 报错{}",e.getMessage());
            }
        });
    }
}
