package com.anyfriend.core.node.system;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.anyfriend.core.utils.SendMessageUtil;
import com.anyfriend.dao.entity.TbUserModelConfigDo;
import com.anyfriend.dao.service.TbUserModelConfigDoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelApiKeyNode implements AsyncNodeActionWithConfig, InterruptableAction {

    private final SendMessageUtil sendMessageUtil;

    private final TbUserModelConfigDoService tbUserModelConfigDoService;
    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        // 正常节点逻辑：更新状态
        TbUserModelConfigDo modelConfigDo = state.value("modelConfig",TbUserModelConfigDo.class).orElse(null);
        String userInput = state.value("userInput",String.class).orElse("");
        Long userId = Long.valueOf(state.value("userId", String.class).orElse(""));
        if (modelConfigDo != null && modelConfigDo.getApiKey() == null){
            modelConfigDo.setApiKey(userInput.trim());
            modelConfigDo.setUserId(userId);
            // 如果已经有 model，继续执行
            sendMessageUtil.sendChatMsg(userId, "设置成功");

            // 把该节点的配置清空
            tbUserModelConfigDoService.clearNodeNumByUserIdAndNodeNum(userId, modelConfigDo.getNodeEnum());

            // 插入数据
            tbUserModelConfigDoService.save(modelConfigDo);
            state.clear();
        }
        return CompletableFuture.completedFuture(Map.of());
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
            sendMessageUtil.sendChatMsg(userId, "请输入apiKey");
            // 返回 InterruptionMetadata 来中断执行
            InterruptionMetadata interruption = InterruptionMetadata.builder(nodeId, state)
                    .addMetadata("message", "等待用户输入apiKey")
                    .addMetadata("node", nodeId)
                    .build();
            log.info("等待用户输入");
            return Optional.of(interruption);
        }
        return Optional.empty();
    }
}
