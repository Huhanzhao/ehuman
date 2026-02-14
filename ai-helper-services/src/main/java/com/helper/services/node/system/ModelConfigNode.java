package com.helper.services.node.system;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.helper.services.entity.TbUserModelConfigDo;
import com.helper.services.enums.QQbotIdEnum;
import com.helper.services.service.TbUserModelConfigDoService;
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
public class ModelConfigNode implements AsyncNodeAction, InterruptableAction {
    private final TbUserModelConfigDoService tbUserModelConfigDoService;
    private final BotContainer botContainer;

    @Override
    public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
        log.info("进入了判断中断方法");
        // 检查是否需要中断
        // 如果状态中没有 model，则中断等待用户输入
        TbUserModelConfigDo modelConfigDo = state.value("modelConfig",TbUserModelConfigDo.class).orElse(null);
        String userInput = state.value("userInput",String.class).orElse("");
        Long userId = state.value("userId",Long.class).orElse(0L);

        if (modelConfigDo== null) {
            botContainer.robots.get(QQbotIdEnum.DEFAULT.getId())
                    .sendPrivateMsg(userId, "请输入要设置模型类别的序号：\n1.chatGpt\n2.claude\n3.grok\n4.deepseek",false);
            // 返回 InterruptionMetadata 来中断执行
            InterruptionMetadata interruption = InterruptionMetadata.builder(nodeId, state)
                    .addMetadata("message", "等待用户输入要设置模型类别的序号")
                    .addMetadata("node", nodeId)
                    .build();
            log.info("等待用户输入节点序号");
            state.input(Map.of("modelConfig", new TbUserModelConfigDo()));
            return Optional.of(interruption);
        }else if (modelConfigDo.getCategory() == null) {
            InterruptionMetadata interruption = null;
            log.info("等待用户输入");
            boolean isInvalid = false;
            try {
                isInvalid = Integer.parseInt(userInput.trim()) > 4 || Integer.parseInt(userInput.trim()) < 1;
            }catch (Exception e){
                botContainer.robots.get(QQbotIdEnum.DEFAULT.getId())
                        .sendPrivateMsg(userId, "请输入正确的序号",false);
                // 返回 InterruptionMetadata 来中断执行
                interruption = InterruptionMetadata.builder(nodeId, state)
                        .addMetadata("message", "请输入正确的序号")
                        .addMetadata("node", nodeId)
                        .build();
                return Optional.of(interruption);
            }

            if (isInvalid){
                botContainer.robots.get(QQbotIdEnum.DEFAULT.getId())
                        .sendPrivateMsg(userId, "请输入正确的序号",false);
                // 返回 InterruptionMetadata 来中断执行
                interruption = InterruptionMetadata.builder(nodeId, state)
                        .addMetadata("message", "请输入正确的序号")
                        .addMetadata("node", nodeId)
                        .build();
                return Optional.of(interruption);
            }

            botContainer.robots.get(QQbotIdEnum.DEFAULT.getId())
                    .sendPrivateMsg(userId, "请输入要设置的节点序号：\n1.意图识别节\n2.好感度控制节\n3.对话生成节点",false);
            // 返回 InterruptionMetadata 来中断执行
            interruption = InterruptionMetadata.builder(nodeId, state)
                    .addMetadata("message", "等待用户输入节点序号")
                    .addMetadata("node", nodeId)
                    .build();
            log.info("等待用户输入节点序号");
            state.input(Map.of("modelConfig", new TbUserModelConfigDo()));
            return Optional.of(interruption);
        } else if (modelConfigDo.getNodeNum() == null) {
            // 返回 InterruptionMetadata 来中断执行
            InterruptionMetadata interruption = null;
            log.info("等待用户输入");
            boolean isInvalid = false;
            try {
                isInvalid = Integer.parseInt(userInput.trim()) > 3 || Integer.parseInt(userInput.trim()) < 1;
            }catch (Exception e){
                botContainer.robots.get(QQbotIdEnum.DEFAULT.getId())
                        .sendPrivateMsg(userId, "请输入正确的节点序号",false);
                // 返回 InterruptionMetadata 来中断执行
                interruption = InterruptionMetadata.builder(nodeId, state)
                        .addMetadata("message", "等待用户输入正确的节点序号")
                        .addMetadata("node", nodeId)
                        .build();
                return Optional.of(interruption);
            }

            if (isInvalid){
                botContainer.robots.get(QQbotIdEnum.DEFAULT.getId())
                        .sendPrivateMsg(userId, "请输入正确的节点序号",false);
                // 返回 InterruptionMetadata 来中断执行
                interruption = InterruptionMetadata.builder(nodeId, state)
                        .addMetadata("message", "等待用户输入正确的节点序号")
                        .addMetadata("node", nodeId)
                        .build();
                return Optional.of(interruption);
            }

            modelConfigDo.setNodeNum(Integer.parseInt(userInput.trim()));

            botContainer.robots.get(QQbotIdEnum.DEFAULT.getId())
                    .sendPrivateMsg(userId, "请输入model名称",false);

            // 返回 InterruptionMetadata 来中断执行
            interruption = InterruptionMetadata.builder(nodeId, state)
                    .addMetadata("message", "等待用户输入model")
                    .addMetadata("node", nodeId)
                    .build();

            state.input(Map.of("modelConfig", modelConfigDo));
            return Optional.of(interruption);
        } else if (modelConfigDo.getModel() == null) {
            modelConfigDo.setModel(userInput.trim());
            botContainer.robots.get(QQbotIdEnum.DEFAULT.getId())
                    .sendPrivateMsg(userId, "请输入baseUrl",false);
            // 返回 InterruptionMetadata 来中断执行
            InterruptionMetadata interruption = InterruptionMetadata.builder(nodeId, state)
                    .addMetadata("message", "等待用户输入baseUrl")
                    .addMetadata("node", nodeId)
                    .build();
            log.info("等待用户输入");
            state.input(Map.of("modelConfig", modelConfigDo));
            return Optional.of(interruption);
        }else if (modelConfigDo.getBaseUrl() == null) {
            modelConfigDo.setBaseUrl(userInput.trim());
            botContainer.robots.get(QQbotIdEnum.DEFAULT.getId())
                    .sendPrivateMsg(userId, "请输入apiKey",false);
            // 返回 InterruptionMetadata 来中断执行
            InterruptionMetadata interruption = InterruptionMetadata.builder(nodeId, state)
                    .addMetadata("message", "等待用户输入apiKey")
                    .addMetadata("node", nodeId)
                    .build();
            log.info("等待用户输入");
            state.input(Map.of("modelConfig", modelConfigDo));
            return Optional.of(interruption);
        }else if (modelConfigDo.getApiKey() == null) {
            modelConfigDo.setApiKey(userInput.trim());
            modelConfigDo.setUserId(userId);

            // 把该节点的配置清空
            tbUserModelConfigDoService.update(Wrappers.<TbUserModelConfigDo>lambdaUpdate()
                    .set(TbUserModelConfigDo::getNodeNum,0)
                    .eq(TbUserModelConfigDo::getUserId,userId)
                    .eq(TbUserModelConfigDo::getNodeNum,modelConfigDo.getNodeNum())
            );

            // 插入数据
            tbUserModelConfigDoService.save(modelConfigDo);
        }
        // 如果已经有 model，继续执行
        botContainer.robots.get(QQbotIdEnum.DEFAULT.getId()).sendPrivateMsg(userId, "设置成功",false);
        return Optional.empty();
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state){
        // 正常节点逻辑：更新状态
        log.info("正常节点逻辑：更新状态");
        return CompletableFuture.completedFuture(Map.of("modelConfigSetStatus", true));
    }
}
