package com.anyfriend.core.bean;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.anyfriend.core.node.SystemInterruptionNode;
import com.anyfriend.core.utils.SendMessageUtil;
import com.anyfriend.dao.entity.TbUserModelConfigDo;
import com.anyfriend.dao.service.TbUserModelConfigDoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.anyfriend.dao.enums.ChatModelCategoryEnum.*;

@Component
@AllArgsConstructor
public class SystemGraph {
    private final SendMessageUtil sendMessageUtil;

    private final TbUserModelConfigDoService tbUserModelConfigDoService;

    @Autowired
    private ObjectMapper mapper;

    public CompiledGraph getApiConfigGraph() {
        // 收集模型类别节点
        SystemInterruptionNode cateGoryNode = SystemInterruptionNode.builder().sendMessageUtil(sendMessageUtil)
                .interruptKey("category")
                .nodeName("cateGoryNode")
                .inputPrompt("请输入要设置模型类别的序号：\n1.chatGpt\n2.claude\n3.grok\n4.deepseek")
                .inputMapping(Map.of("1", GPT.getCategory(),
                        "2", CLAUDE.getCategory(),
                        "3",GROK.getCategory(),
                        "4",DEEP_SEEK.getCategory())) // 1.chatGpt 2.claude 3.grok 4.deepseek
                .build();

        // 收集模型名字节点
        SystemInterruptionNode modelNameNode = SystemInterruptionNode.builder().sendMessageUtil(sendMessageUtil)
                .interruptKey("model")
                .nodeName("modelNameNode")
                .inputPrompt("请输入model型号")
                .build();
        // 收集baseUrl
        SystemInterruptionNode baseUrlNode = SystemInterruptionNode.builder().sendMessageUtil(sendMessageUtil)
                .interruptKey("baseUrl")
                .nodeName("baseUrlNode")
                .inputPrompt("请输入baseUrl")
                .afterLlmCallback((state, llmOutput) -> {
                    // 如果包含v1 去掉v1
                    state.updateState(Map.of("baseUrl",state.value("baseUrl",String.class).orElse("").replace("/v1","")));
                })
                .build();

        // 收集是否需要代理
        SystemInterruptionNode proxyNode = SystemInterruptionNode.builder().sendMessageUtil(sendMessageUtil)
                .interruptKey("needProxy")
                .nodeName("proxyNode")
                .inputPrompt("是否需要代理,请输入序号：\n1.是\n2.否")
                .inputMapping(Map.of("1", true,
                        "2", false))
                .build();

        // 收集模型名字节点
        SystemInterruptionNode apiKeyNode = SystemInterruptionNode.builder().sendMessageUtil(sendMessageUtil)
                .interruptKey("apiKey")
                .nodeName("apiKeyNode")
                .inputPrompt("请输入apiKey")
                .afterLlmCallback((state, llmOutput) -> {
                    // 保存api配置
                    Map<String,Object> data = state.data();
                    TbUserModelConfigDo modelConfigDo = mapper.convertValue(state.data(), TbUserModelConfigDo.class);
                    // 插入数据
                    tbUserModelConfigDoService.save(modelConfigDo);

                    sendMessageUtil.sendSystemMsg(Long.valueOf(data.get("userId").toString()), "设置成功");
                    state.clear();
                })
                .build();

        return getSystemGraph(List.of(cateGoryNode, modelNameNode, baseUrlNode, proxyNode, apiKeyNode),
                List.of("category", "model", "baseUrl", "apiKey", "needProxy"));
    }


    // 获取系统收集信息中断节点图
    public CompiledGraph getSystemGraph(List<SystemInterruptionNode> nodes, List<String> keys) {
        // 配置 KeyStrategyFactory 不配置会导致人类反馈拿不到状态
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
            for (String key : keys) {
                keyStrategyHashMap.put(key, new ReplaceStrategy());
            }
            keyStrategyHashMap.put("userInput", new ReplaceStrategy());
            keyStrategyHashMap.put("userId", new ReplaceStrategy());
            return keyStrategyHashMap;
        };

        StateGraph stateGraph = new StateGraph(keyStrategyFactory);

        try {
            for (int i = 0; i < nodes.size(); i++){
                SystemInterruptionNode node = nodes.get(i);
                stateGraph.addNode(node.getNodeName(), node);
                if (i == 0){
                    stateGraph.addEdge(StateGraph.START, node.getNodeName());
                    stateGraph.addEdge(node.getNodeName(), nodes.get(i + 1).getNodeName());
                }else if (i == nodes.size() - 1){
                    stateGraph.addEdge(node.getNodeName(), StateGraph.END);
                }else {
                    stateGraph.addEdge(node.getNodeName(), nodes.get(i + 1).getNodeName());
                }
            }

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
