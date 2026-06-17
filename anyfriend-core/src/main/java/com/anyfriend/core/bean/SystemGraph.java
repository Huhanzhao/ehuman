package com.anyfriend.core.bean;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.anyfriend.core.config.NodeStatePersistenceListener;
import com.anyfriend.core.node.system.SystemInterruptionNode;
import com.anyfriend.core.node.system.SwitchNodeApiConfigNode;
import com.anyfriend.core.utils.SendMessageUtil;
import com.anyfriend.dao.entity.TbUserModelConfigDo;
import com.anyfriend.dao.entity.TbUserReferAudioDo;
import com.anyfriend.dao.enums.NodeEnum;
import com.anyfriend.dao.service.TbUserModelConfigDoService;
import com.anyfriend.dao.service.TbUserReferAudioDoService;
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
    private final TbUserReferAudioDoService tbUserReferAudioDoService;

    @Autowired
    private SwitchNodeApiConfigNode switchNodeApiConfigNode;

    @Autowired
    private ObjectMapper mapper;

    public CompiledGraph getApiConfigGraph() {
        // 收集模型类别节点
        SystemInterruptionNode cateGoryNode = SystemInterruptionNode.builder().sendMessageUtil(sendMessageUtil)
                .interruptKey("category")
                .nodeName("cateGoryNode")
                .inputPrompt("请输入要设置模型类别的序号：\n1.Gpt协议聊天模型\n2.claude协议聊天模型\n3.gpt协议图片识别\n4.TTS文字转语音")
                .inputMapping(Map.of("1", GPT.getCategory(),
                        "2", CLAUDE.getCategory(),
                        "3",IMAGE.getCategory(),
                        "4",TTS.getCategory()))
                .build();

        // 收集模型名字节点
        SystemInterruptionNode modelNameNode = SystemInterruptionNode.builder().sendMessageUtil(sendMessageUtil)
                .interruptKey("model")
                .nodeName("modelNameNode")
                .inputPrompt("请输入model型号,如果是TTS类型，请输入插件name")
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

    public CompiledGraph getChangeModelCompiledGraph(){
        // 1. 需要切换的节点类型 主，辅助，图片识别，语音合成
        SystemInterruptionNode nodeTypeNode = SystemInterruptionNode.builder()
                .sendMessageUtil(sendMessageUtil)
                .inputPrompt("请输入要切换的节点类型序号：\n1.主节点\n2.辅助节点\n3.图片识别节点\n4.语音合成节点")
                .inputMapping(Map.of("1", NodeEnum.MASTER, "2", NodeEnum.AUXILIARY, "3", NodeEnum.IMAGE_ANALYSIS, "4", NodeEnum.TTS))
                .nodeName("nodeTypeNode")
                .interruptKey("nodeType").build();

        return getSystemGraph(List.of(nodeTypeNode, switchNodeApiConfigNode),List.of("nodeType","apiConfigIndex"));
    }


    // 获取系统收集信息中断节点图
    private CompiledGraph getSystemGraph(List<SystemInterruptionNode> nodes, List<String> keys) {
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

    public CompiledGraph getReferAudioConfigGraph() {
        // 参考音频类型
        SystemInterruptionNode fileTypeNode = SystemInterruptionNode.builder()
                .sendMessageUtil(sendMessageUtil)
                .inputPrompt("参考音频是本地的还是在线链接呢？请输入序号：\n1.本地\n2.在线链接")
                .inputMapping(Map.of("1","local","2","online"))
                .nodeName("fileTypeNode")
                .interruptKey("type").build();


        // 文件名
        SystemInterruptionNode fileNameNode = SystemInterruptionNode.builder()
                .sendMessageUtil(sendMessageUtil)
                .inputPrompt("请输入文件全名（包含后缀名）")
                .nodeName("fileNameNode")
                .interruptKey("referAudioName").build();

        // 在线链接
        SystemInterruptionNode audioUrlNode = SystemInterruptionNode.builder()
                .sendMessageUtil(sendMessageUtil)
                .inputPrompt("请输入音频公网url\n(不需要公网链接的插件可以随便填，自带插件indextts2需要公网链接，mimoTTS使用本地文件不需要公网链接,自定义插件看情况使用)")
                .nodeName("audioUrlNode")
                .interruptKey("referAudioUrl")
                .afterLlmCallback((state,llmOutPut) -> {
                    // 保存配置
                    Map<String,Object> data = state.data();
                    TbUserReferAudioDo tbUserReferAudioDo = mapper.convertValue(state.data(), TbUserReferAudioDo.class);
                    // 插入数据
                    tbUserReferAudioDoService.save(tbUserReferAudioDo);

                    sendMessageUtil.sendSystemMsg(Long.valueOf(data.get("userId").toString()), "设置成功");
                    state.clear();
                })
                .build();

        return getSystemGraph(List.of(fileTypeNode,fileNameNode,audioUrlNode),List.of("type","referAudioName","referAudioUrl"));
    }

    public CompiledGraph getSwitchReferAudioGraph() {
        // 需要切换本地参考音频还是在线音频，当前声音插件是。。。 请注意音频类型与插件的适配性，默认插件 indextts2必须使用在线音频，mimoTTS必须使用本地音频

        // 请选择要切换的音频

        return getSystemGraph(List.of(),List.of("type"));
    }
}
