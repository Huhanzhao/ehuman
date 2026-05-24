package com.bot.service.node.system;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.bot.service.entity.TbUserModelConfigDo;
import com.bot.service.enums.QQbotIdEnum;
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
public class ModelBaseUrlNode implements AsyncNodeActionWithConfig, InterruptableAction {

    private final BotContainer botContainer;
    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        // 正常节点逻辑：更新状态
        TbUserModelConfigDo modelConfigDo = state.value("modelConfig",TbUserModelConfigDo.class).orElse(null);
        String userInput = state.value("userInput",String.class).orElse("");
        if (modelConfigDo != null && modelConfigDo.getBaseUrl() == null){
            modelConfigDo.setBaseUrl(userInput.trim());
            state.updateState(Map.of("modelConfig", modelConfigDo,"userInput", ""));
        }
        return CompletableFuture.completedFuture(Map.of("modelConfigSetStatus", "ModelBaseUrlNodeSuccess"));
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
            botContainer.robots.get(QQbotIdEnum.DEFAULT.getId())
                    .sendPrivateMsg(userId, "请输入baseUrl",false);
            // 返回 InterruptionMetadata 来中断执行
            InterruptionMetadata interruption = InterruptionMetadata.builder(nodeId, state)
                    .addMetadata("message", "等待用户输入baseUrl")
                    .addMetadata("node", nodeId)
                    .build();
            log.info("等待用户输入");
            return Optional.of(interruption);
        }
        return Optional.empty();
    }
}
