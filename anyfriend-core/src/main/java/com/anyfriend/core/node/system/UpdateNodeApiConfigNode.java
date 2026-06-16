package com.anyfriend.core.node.system;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.anyfriend.core.service.BeforeLlmCallback;
import com.anyfriend.core.utils.SendMessageUtil;
import com.anyfriend.dao.entity.TbUserModelConfigDo;
import com.anyfriend.dao.enums.ChatModelCategoryEnum;
import com.anyfriend.dao.enums.NodeEnum;
import com.anyfriend.dao.service.TbUserModelConfigDoService;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.anyfriend.dao.enums.ChatModelCategoryEnum.IMAGE;
import static com.anyfriend.dao.enums.ChatModelCategoryEnum.TTS;

/**
 * @Author: huhanzhao
 * @CreateTime: 2026-06-16
 * @Description: 更新节点的apiconfig
 */
@Data
@Slf4j
@Component
public class UpdateNodeApiConfigNode extends SystemInterruptionNode{

    @Autowired
    private SendMessageUtil sendMessageUtil;

    private String interruptKey = "apiConfigIndex";

    private String nodeName = "updateNodeApiConfigNode";

    @Autowired
    private TbUserModelConfigDoService tbUserModelConfigDoService;

    @Override
    public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
        log.info("进入了{}节点中断方法",nodeName);
        // 检查是否需要中断
        String userInput = state.value("userInput",String.class).orElse("").trim();
        Long userId = Long.valueOf(state.value("userId", String.class).orElse(""));
        InterruptionMetadata interruption = null;
        NodeEnum nodeEnum = state.value("nodeType", NodeEnum.class).get();
        ChatModelCategoryEnum categoryEnum = null;
        if (nodeEnum.equals(NodeEnum.IMAGE_ANALYSIS)){
            categoryEnum = IMAGE;
        }
        if (nodeEnum.equals(NodeEnum.TTS)){
            categoryEnum = TTS;
        }
        List<TbUserModelConfigDo> modelConfigList = tbUserModelConfigDoService.getDisableApiConfigByCategory(userId, categoryEnum);
        if (userInput.isEmpty()) {
            // 中断等待用户输入
            StringBuilder userMessageBuilder = new StringBuilder("当前可切换的api配置\n");
            if (modelConfigList.isEmpty()){
                userMessageBuilder.append("无");
            }
            for (int i = 0; i < modelConfigList.size(); i++) {
                TbUserModelConfigDo tbUserModelConfigDo = modelConfigList.get(i);
                userMessageBuilder.append(i + 1).append(" . model:")
                        .append(tbUserModelConfigDo.getModel()).append("\n")
                        .append("baseUrl:").append(tbUserModelConfigDo.getBaseUrl()).append("\n\n");
            }
            sendMessageUtil.sendSystemMsg(userId, userMessageBuilder.toString().trim());
            // 返回 InterruptionMetadata 来中断执行
            interruption = InterruptionMetadata.builder(nodeId, state)
                    .addMetadata("message", nodeName + "节点中断")
                    .addMetadata("node", nodeId)
                    .build();
            log.info("{}节点中断",nodeName);
            return Optional.of(interruption);
        }

        // 判断用户输入是否合法
        int index = 0;
        try {
            index = Long.valueOf(userInput).intValue();
        }catch(Exception exception){
            log.error("非法输入");
        }

        if (index <= 0 || index > modelConfigList.size()){
            sendMessageUtil.sendSystemMsg(userId, "请按照要求输入");
            // 返回 InterruptionMetadata 来中断执行
            interruption = InterruptionMetadata.builder(nodeId, state)
                    .addMetadata("message", nodeName + "节点中断")
                    .addMetadata("node", nodeId)
                    .build();
            return Optional.of(interruption);
        }

        // 保存
        TbUserModelConfigDo tbUserModelConfigDo = modelConfigList.get(index - 1);
        tbUserModelConfigDo.setNodeEnum(nodeEnum);
        tbUserModelConfigDoService.changeApiModelById(userId,tbUserModelConfigDo,categoryEnum);
        // 3. 切换完毕
        sendMessageUtil.sendSystemMsg(userId,"更新成功");
        return Optional.empty();
    }
}
