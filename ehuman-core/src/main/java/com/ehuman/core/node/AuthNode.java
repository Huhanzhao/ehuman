package com.ehuman.core.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.ehuman.core.service.BotAccessService;
import com.ehuman.core.service.EffectiveConfigResolver;
import com.ehuman.core.service.WelcomeService;
import com.ehuman.core.utils.SendMessageUtil;
import com.ehuman.dao.entity.TbAiChatMemoryDo;
import com.ehuman.dao.entity.TbChatConfigDo;
import com.ehuman.dao.entity.TbUserModelConfigDo;
import com.ehuman.dao.enums.MessageTypeEnum;
import com.ehuman.dao.service.TbAiChatMemoryService;
import com.ehuman.dao.service.TbChatConfigDoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Component
public class AuthNode implements NodeAction {

    private final EffectiveConfigResolver effectiveConfigResolver;

    private final TbChatConfigDoService tbChatConfigDoService;

    private final SendMessageUtil sendMessageUtil;

    private final TbAiChatMemoryService tbAiChatMemoryService;

    private final BotAccessService botAccessService;

    private final WelcomeService welcomeService;

    @Override
    public Map<String, Object> apply(OverAllState state){
        // todo 启动之后校验是否齐全，之后跳过校验
        Long userId = state.value("userId",Long.class).orElse(0L);
        String userInput = state.value("userInput",String.class).orElse("");

        List<TbUserModelConfigDo> list = effectiveConfigResolver.getUsableModelConfigs(userId);
        if (list.isEmpty()) {
            if (botAccessService.getOwnerId() != null) {
                sendMessageUtil.sendSystemMsg(userId, botAccessService.contactOwnerMessage());
            } else {
                sendMessageUtil.sendSystemMsg(userId, "没有可用apikey\n输入#号查看菜单\n输入#快速开始 进入配置deepseek模型快速开始引导");
            }
            throw new RuntimeException("没有可用apikey");
        }

        TbChatConfigDo one = effectiveConfigResolver.getChatConfig(userId);
        if (one == null && !botAccessService.isSharedKeyUser(userId)) {
            TbChatConfigDo tb = new TbChatConfigDo();
            tb.setUserId(userId);
            tb.setTemperature(0.7);
            tb.setTopP(1.0);
            tbChatConfigDoService.save(tb);
        }

        // 必须在保存用户消息之前判断首次聊天
        welcomeService.markFirstChatIfNeeded(userId);

        if(!userInput.startsWith("[CQ:image")){
            // 不是图片直接保存 是图片在图片节点解析之后保存
            tbAiChatMemoryService.save(new TbAiChatMemoryDo().setMemoryId(userId)
                    .setRole(MessageTypeEnum.USER_MESSAGE).setMessage(userInput));
        }
        return Map.of("auth",true);
    }
}
