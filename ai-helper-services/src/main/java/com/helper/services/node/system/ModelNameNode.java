package com.helper.services.node.system;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.helper.services.entity.TbUserModelConfigDo;
import com.helper.services.enums.QQbotIdEnum;
import com.mikuac.shiro.core.BotContainer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelNameNode implements AsyncNodeActionWithConfig, InterruptableAction {

    private final BotContainer botContainer;
    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        // 正常节点逻辑：更新状态
        TbUserModelConfigDo modelConfigDo = state.value("modelConfig",TbUserModelConfigDo.class).orElse(null);
        String userInput = state.value("userInput",String.class).orElse("");
        if (modelConfigDo != null && modelConfigDo.getModel() == null){
            modelConfigDo.setModel(userInput.trim());
            state.updateState(Map.of("modelConfig", modelConfigDo,"userInput", ""));
        }
        return CompletableFuture.completedFuture(Map.of("modelConfigSetStatus", "ModelNameNodeSuccess"));
    }

    @Override
    public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
        log.info("进入了判断ModelNameNode中断方法");
        // 检查是否需要中断
        // 如果状态中没有 model，则中断等待用户输入
        TbUserModelConfigDo modelConfigDo = state.value("modelConfig",TbUserModelConfigDo.class).orElse(null);
        String userInput = state.value("userInput",String.class).orElse("");
        Long userId = Long.valueOf(state.value("userId", String.class).orElse(""));
        if (userInput.isEmpty()){
            InterruptionMetadata interruption = null;
            log.info("等待用户输入");
            botContainer.robots.get(QQbotIdEnum.DEFAULT.getId())
                    .sendPrivateMsg(userId, "请输入model名称",false);

            // 返回 InterruptionMetadata 来中断执行
            interruption = InterruptionMetadata.builder(nodeId, state)
                    .addMetadata("message", "等待用户输入model")
                    .addMetadata("node", nodeId)
                    .build();

            return Optional.of(interruption);
        }
        return Optional.empty();
    }
}
