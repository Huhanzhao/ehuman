package com.ehuman.core.bean;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.ehuman.core.node.system.CollectReferAudioTypeNode;
import com.ehuman.core.node.system.SwitchNodeApiConfigNode;
import com.ehuman.core.node.system.SwitchReferAudioNode;
import com.ehuman.core.node.system.SystemInterruptionNode;
import com.ehuman.core.utils.SendMessageUtil;
import com.ehuman.dao.entity.TbBotPromptDo;
import com.ehuman.dao.entity.TbUserModelConfigDo;
import com.ehuman.dao.entity.TbUserReferAudioDo;
import com.ehuman.dao.enums.ChatModelCategoryEnum;
import com.ehuman.dao.enums.LlmTypeEnum;
import com.ehuman.dao.service.TbBotPromptDoService;
import com.ehuman.dao.service.TbUserModelConfigDoService;
import com.ehuman.dao.service.TbUserReferAudioDoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ehuman.dao.enums.ChatModelCategoryEnum.*;

@Component
@AllArgsConstructor
public class SystemGraph {
    private final SendMessageUtil sendMessageUtil;

    private final TbUserModelConfigDoService tbUserModelConfigDoService;

    @Autowired
    private final TbUserReferAudioDoService tbUserReferAudioDoService;

    @Autowired
    private final TbBotPromptDoService tbBotPromptDoService;

    @Autowired
    private SwitchNodeApiConfigNode switchNodeApiConfigNode;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private CollectReferAudioTypeNode collectReferAudioTypeNode;

    @Autowired
    private SwitchReferAudioNode switchReferAudioNode;

    public CompiledGraph getApiConfigGraph() {
        // 收集模型类别节点
        SystemInterruptionNode cateGoryNode = SystemInterruptionNode.builder().sendMessageUtil(sendMessageUtil)
                .interruptKey("category")
                .nodeName("cateGoryNode")
                .inputPrompt("请输入要设置模型类别的序号：\n1.Gpt协议聊天模型\n2.claude协议聊天模型\n3.gpt协议图片识别\n4.TTS文字转语音\n5.Embedding模型")
                .inputMapping(Map.of("1", GPT.getCategory(),
                        "2", CLAUDE.getCategory(),
                        "3",IMAGE.getCategory(),
                        "4",TTS.getCategory(),
                        "5", ChatModelCategoryEnum.EMBEDDING.getCategory()))
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
                .build();

        // 是否立即启用
        SystemInterruptionNode enableNode = SystemInterruptionNode.builder().sendMessageUtil(sendMessageUtil)
                .interruptKey("enable")
                .nodeName("enableNode")
                .inputPrompt("是否立即启用,请输入序号：\n1.是\n2.否")
                .inputMapping(Map.of("1", true,
                        "2", false))
                .afterLlmCallback((state, llmOutput) -> {
                    // 保存api配置
                    Map<String,Object> data = state.data();
                    TbUserModelConfigDo modelConfigDo = mapper.convertValue(state.data(), TbUserModelConfigDo.class);
                    // 是否启用
                    Boolean enable = (Boolean) data.get("enable");
                    if(enable){
                        switch (ChatModelCategoryEnum.getByCategory(modelConfigDo.getCategory())){
                            case GPT, CLAUDE -> {  // 文字模型
                                tbUserModelConfigDoService.clearNodeNumByUserIdAndNodeNum(modelConfigDo.getUserId(), List.of(LlmTypeEnum.MASTER, LlmTypeEnum.AUXILIARY));
                                modelConfigDo.setLlmTypeEnum(LlmTypeEnum.MASTER);
                            }
                            case IMAGE -> {
                                tbUserModelConfigDoService.clearNodeNumByUserIdAndNodeNum(modelConfigDo.getUserId(),List.of(LlmTypeEnum.IMAGE_ANALYSIS));
                                modelConfigDo.setLlmTypeEnum(LlmTypeEnum.IMAGE_ANALYSIS);
                            }
                            case TTS -> {
                                tbUserModelConfigDoService.clearNodeNumByUserIdAndNodeNum(modelConfigDo.getUserId(),List.of(LlmTypeEnum.TTS));
                                modelConfigDo.setLlmTypeEnum(LlmTypeEnum.TTS);
                            }
                        }
                        // 开启当前api
                        tbUserModelConfigDoService.save(modelConfigDo);
                    }else{
                        // 插入数据
                        tbUserModelConfigDoService.save(modelConfigDo);
                    }
                    sendMessageUtil.sendSystemMsg(Long.valueOf(data.get("userId").toString()), "设置成功");
                    state.clear();
                })
                .build();

        return getSystemGraph(List.of(cateGoryNode, modelNameNode, baseUrlNode, proxyNode, apiKeyNode,enableNode),
                List.of("category", "model", "baseUrl", "apiKey", "needProxy","enable"));
    }

    public CompiledGraph getChangeModelCompiledGraph(){
        // 1. 需要切换的节点类型 主，辅助，图片识别，语音合成
        SystemInterruptionNode nodeTypeNode = SystemInterruptionNode.builder()
                .sendMessageUtil(sendMessageUtil)
                .inputPrompt("请输入要切换的节点类型序号：\n1.主节点\n2.辅助节点\n3.图片识别节点\n4.语音合成节点\n5.Embedding模型")
                .inputMapping(Map.of("1", LlmTypeEnum.MASTER.name(), "2", LlmTypeEnum.AUXILIARY.name(), "3", LlmTypeEnum.IMAGE_ANALYSIS.name(), "4", LlmTypeEnum.TTS.name(), "5", LlmTypeEnum.EMBEDDING.name()))
                .nodeName("nodeTypeNode")
                .interruptKey("nodeType").build();

        return getSystemGraph(List.of(nodeTypeNode, switchNodeApiConfigNode),List.of("nodeType","apiConfigIndex"));
    }


    public CompiledGraph getCreateRoleGraph() {
        // 收集人设名称
        SystemInterruptionNode roleNameNode = SystemInterruptionNode.builder()
                .sendMessageUtil(sendMessageUtil)
                .inputPrompt("请输入人设名称")
                .nodeName("roleNameNode")
                .interruptKey("name").build();

        // 收集人设内容
        SystemInterruptionNode roleContentNode = SystemInterruptionNode.builder()
                .sendMessageUtil(sendMessageUtil)
                .inputPrompt("请输入人设prompt内容")
                .nodeName("roleContentNode")
                .interruptKey("content")
                .afterLlmCallback((state, llmOutput) -> {
                    Map<String, Object> data = state.data();
                    TbBotPromptDo promptDo = new TbBotPromptDo();
                    promptDo.setName(data.get("name").toString());
                    promptDo.setContent(data.get("content").toString());
                    promptDo.setEnable(false);
                    tbBotPromptDoService.save(promptDo);

                    sendMessageUtil.sendSystemMsg(Long.valueOf(data.get("userId").toString()), "人设创建成功");
                    state.clear();
                })
                .build();

        return getSystemGraph(List.of(roleNameNode, roleContentNode), List.of("name", "content"));
    }

    // 开启语音合成插入收集默认语音合成插件apikey
    public CompiledGraph getStartTtsGraph() {
        // 收集人设名称
        SystemInterruptionNode roleNameNode = SystemInterruptionNode.builder()
                .sendMessageUtil(sendMessageUtil)
                .inputPrompt("请输入")
                .nodeName("roleNameNode")
                .interruptKey("name").build();

        // 收集人设内容
        SystemInterruptionNode roleContentNode = SystemInterruptionNode.builder()
                .sendMessageUtil(sendMessageUtil)
                .inputPrompt("请输入人设prompt内容")
                .nodeName("roleContentNode")
                .interruptKey("content")
                .afterLlmCallback((state, llmOutput) -> {
                    Map<String, Object> data = state.data();
                    TbBotPromptDo promptDo = new TbBotPromptDo();
                    promptDo.setName(data.get("name").toString());
                    promptDo.setContent(data.get("content").toString());
                    promptDo.setEnable(false);
                    tbBotPromptDoService.save(promptDo);

                    sendMessageUtil.sendSystemMsg(Long.valueOf(data.get("userId").toString()), "人设创建成功");
                    state.clear();
                })
                .build();

        return getSystemGraph(List.of(roleNameNode, roleContentNode), List.of("name", "content"));
    }

    /**
     * 获取系统收集信息中断节点图
     * @param nodes 节点list
     * @param keys 除了userId userInput额外的replace key
     * @return 编译好的graph
     */
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
        return getSystemGraph(List.of(collectReferAudioTypeNode,switchReferAudioNode),List.of("type"));
    }

    public CompiledGraph getQuickStartGraph() {
        // 文件名
        SystemInterruptionNode modelNameNode = SystemInterruptionNode.builder()
                .sendMessageUtil(sendMessageUtil)
                .inputPrompt("请输入deepseek模型名称")
                .nodeName("modelNameNode")
                .interruptKey("model").build();

        // 参考音频类型
        SystemInterruptionNode apiKeyNode = SystemInterruptionNode.builder()
                .sendMessageUtil(sendMessageUtil)
                .inputPrompt("请输入deepseek apikey")
                .nodeName("apiKeyNode")
                .interruptKey("apiKey")
                .afterLlmCallback((state,llmOutPut) -> {
                    // 保存配置
                    Map<String,Object> data = state.data();
                    Long userId = Long.valueOf(data.get("userId").toString());

                    TbUserModelConfigDo tbUserModelConfigDo = mapper.convertValue(state.data(), TbUserModelConfigDo.class);
                    tbUserModelConfigDo.setCategory(GPT.getCategory()).setBaseUrl("https://api.deepseek.com")
                            .setNeedProxy(false).setLlmTypeEnum(LlmTypeEnum.MASTER);
                    TbUserModelConfigDo tbUserModelConfigDo2 = new TbUserModelConfigDo();
                    BeanUtils.copyProperties(tbUserModelConfigDo,tbUserModelConfigDo2);
                    tbUserModelConfigDo2.setLlmTypeEnum(LlmTypeEnum.AUXILIARY);
                    // 插入数据
                    tbUserModelConfigDoService.saveBatch(List.of(tbUserModelConfigDo,tbUserModelConfigDo2));

                    sendMessageUtil.sendSystemMsg(userId, "设置成功 可以开始聊天了");
                    state.clear();
                })
                .build();

        return getSystemGraph(List.of(modelNameNode,apiKeyNode),List.of("apikey","model"));

    }
}
