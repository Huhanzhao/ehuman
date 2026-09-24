package com.ehuman.core.node.system;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.ehuman.core.utils.SendMessageUtil;
import com.ehuman.dao.entity.TbUserModelConfigDo;
import com.ehuman.dao.enums.LlmTypeEnum;
import com.ehuman.dao.service.TbUserModelConfigDoService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @Author: huhanzhao
 * @CreateTime: 2026-06-16
 * @Description: 更新节点的apiconfig
 */
@Data
@Slf4j
@Component
public class CollectReferAudioTypeNode extends SystemInterruptionNode{

    @Autowired
    private SendMessageUtil sendMessageUtil;

    private String interruptKey = "type";

    private String nodeName = "collectReferAudioTypeNode";

    @Autowired
    private TbUserModelConfigDoService tbUserModelConfigDoService;



    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        String userInput = state.value("userInput",String.class).orElse("").trim();
        Integer index = Long.valueOf(userInput).intValue();
        state.updateState(Map.of("type",index == 1? "local":"online"));
        state.updateState(Map.of("userInput", ""));
        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
        log.info("进入了{}节点中断方法",nodeName);
        // 检查是否需要中断
        String userInput = state.value("userInput",String.class).orElse("").trim();
        Long userId = Long.valueOf(state.value("userId", String.class).orElse(""));
        InterruptionMetadata interruption = null;
        if (userInput.isEmpty()) {
            // 中断等待用户输入
            TbUserModelConfigDo byUserIdAndNodeNum = tbUserModelConfigDoService.getByUserIdAndNodeNum(userId, LlmTypeEnum.TTS);
            String enableTTSPlugin = "没有启用的TTS插件";
            if (byUserIdAndNodeNum != null){
                enableTTSPlugin = byUserIdAndNodeNum.getModel();
            }
            String userPrompt = """
                    需要切换到本地参考音频还是在线音频?
                    1.本地
                    2.在线
                    当前声音插件是: """ + enableTTSPlugin + """
                    (请注意音频类型与插件的适配性，系统默认自带插件 indexTTS2必须使用在线音频，mimoTTS必须使用本地音频)""";
            sendMessageUtil.sendSystemMsg(userId, userPrompt);
            // 返回 InterruptionMetadata 来中断执行
            interruption = InterruptionMetadata.builder(nodeId, state)
                    .addMetadata("message", nodeName + "节点中断")
                    .addMetadata("node", nodeId)
                    .build();
            log.info("{}节点中断",nodeName);
            return Optional.of(interruption);
        }

        // 判断用户输入是否合法
        int index;
        try {
            index = Long.valueOf(userInput).intValue();
        }catch(Exception exception){
            log.error("非法输入");
            index = -1;
        }

        if (index <= 0 || index > 2){
            sendMessageUtil.sendSystemMsg(userId, "请按照要求输入");
            // 返回 InterruptionMetadata 来中断执行
            interruption = InterruptionMetadata.builder(nodeId, state)
                    .addMetadata("message", nodeName + "节点中断")
                    .addMetadata("node", nodeId)
                    .build();
            return Optional.of(interruption);
        }
        return Optional.empty();
    }
}
