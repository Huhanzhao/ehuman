package com.anyfriend.core.bean;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.anyfriend.core.node.SystemInterruptionNode;
import com.anyfriend.core.utils.SendMessageUtil;
import com.anyfriend.dao.entity.TbUserModelConfigDo;
import com.anyfriend.dao.service.TbUserModelConfigDoService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static com.anyfriend.dao.enums.ChatModelCategoryEnum.*;

@Component
@AllArgsConstructor
public class SystemGraph {
    private final SendMessageUtil sendMessageUtil;

    private final TbUserModelConfigDoService tbUserModelConfigDoService;

    public CompiledGraph getApiConfigGraph() {
        // 配置 KeyStrategyFactory 不配置会导致人类反馈拿不到状态
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
            keyStrategyHashMap.put("userInput", new ReplaceStrategy());
            keyStrategyHashMap.put("userId", new ReplaceStrategy());
            keyStrategyHashMap.put("cateGory", new ReplaceStrategy());
            keyStrategyHashMap.put("modelName", new ReplaceStrategy());
            keyStrategyHashMap.put("baseUrl", new ReplaceStrategy());
            keyStrategyHashMap.put("apiKey", new ReplaceStrategy());
            keyStrategyHashMap.put("proxy", new ReplaceStrategy());
            return keyStrategyHashMap;
        };


        StateGraph stateGraph = new StateGraph(keyStrategyFactory);

        // 收集模型类别节点
        SystemInterruptionNode cateGoryNode = SystemInterruptionNode.builder().sendMessageUtil(sendMessageUtil)
                .interruptKey("cateGory")
                .nodeName("收集模型类别节点")
                .inputPrompt("请输入要设置模型类别的序号：\n1.chatGpt\n2.claude\n3.grok\n4.deepseek")
                .inputMapping(Map.of("1", GPT.getCategory(),
                        "2", CLAUDE.getCategory(),
                        "3",GROK.getCategory(),
                        "4",DEEP_SEEK.getCategory())) // 1.chatGpt 2.claude 3.grok 4.deepseek
                .build();

        // 收集模型名字节点
        SystemInterruptionNode modelNameNode = SystemInterruptionNode.builder().sendMessageUtil(sendMessageUtil)
                .interruptKey("modelName")
                .nodeName("收集模型型号节点")
                .inputPrompt("请输入model型号")
                .build();
        // 收集baseUrl
        SystemInterruptionNode baseUrlNode = SystemInterruptionNode.builder().sendMessageUtil(sendMessageUtil)
                .interruptKey("baseUrl")
                .nodeName("收集baseUrl节点")
                .inputPrompt("请输入baseUrl")
                .afterLlmCallback((state, llmOutput) -> {
                    // 如果包含v1 去掉v1
                    state.updateState(Map.of("baseUrl",state.value("baseUrl",String.class).orElse("").replace("/v1","")));
                })
                .build();

        // 收集是否需要代理
        SystemInterruptionNode proxyNode = SystemInterruptionNode.builder().sendMessageUtil(sendMessageUtil)
                .interruptKey("proxy")
                .nodeName("收集是否需要代理节点")
                .inputPrompt("是否需要代理,请输入序号：\n1.是\n2.否")
                .inputMapping(Map.of("1", true,
                        "2", false))
                .build();

        // 收集模型名字节点
        SystemInterruptionNode apiKeyNode = SystemInterruptionNode.builder().sendMessageUtil(sendMessageUtil)
                .interruptKey("apiKey")
                .nodeName("收集apiKey节点")
                .inputPrompt("请输入apiKey")
                .afterLlmCallback((state, llmOutput) -> {
                    // 保存api配置
                    Map<String,Object> data = state.data();
                    TbUserModelConfigDo modelConfigDo = new TbUserModelConfigDo()
                            .setModel(data.get("modelName").toString())
                            .setCategory(data.get("cateGory").toString())
                            .setBaseUrl(data.get("baseUrl").toString())
                            .setApiKey(data.get("apiKey").toString())
                            .setUserId((Long.valueOf(data.get("userId").toString())))
                            .setNeedProxy(Boolean.parseBoolean(data.get("proxy").toString()));
                    // 插入数据
                    tbUserModelConfigDoService.save(modelConfigDo);

                    sendMessageUtil.sendSystemMsg(Long.valueOf(data.get("userId").toString()), "设置成功");
                    state.clear();
                })
                .build();


        try {
            stateGraph
                    .addNode("cateGoryNode", cateGoryNode)
                    .addNode("modelNameNode", modelNameNode)
                    .addNode("baseUrlNode", baseUrlNode)
                    .addNode("proxyNode", proxyNode)
                    .addNode("apiKeyNode", apiKeyNode)

                    .addEdge( StateGraph.START, "cateGoryNode")
                    .addEdge( "cateGoryNode", "modelNameNode")
                    .addEdge( "modelNameNode", "baseUrlNode")
                    .addEdge( "baseUrlNode", "proxyNode")
                    .addEdge( "proxyNode", "apiKeyNode")
                    .addEdge("apiKeyNode", StateGraph.END);

            // 配置内存保存器（用于状态持久化）
            var saver = new MemorySaver();
            var compileConfig = CompileConfig.builder()
                    .saverConfig(SaverConfig.builder()
                            .register(saver)
                            .build())
                    .build();
            // 生成 PUML 文件
            GraphRepresentation graphRepresentation = stateGraph.getGraph(GraphRepresentation.Type.PLANTUML,
                    "API Config flow");
            Path pumlDir = Paths.get("ai-bot-service/src/main/resources/puml");
            Files.createDirectories(pumlDir);
            Files.writeString(pumlDir.resolve("ApiConfigGraph.puml"), graphRepresentation.content());

            // 不再需要 interruptBefore 配置，中断由 InterruptableAction 控制
            return stateGraph.compile(compileConfig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
