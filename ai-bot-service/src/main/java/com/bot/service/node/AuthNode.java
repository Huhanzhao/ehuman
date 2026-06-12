package com.bot.service.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.bot.service.entity.TbChatConfigDo;
import com.bot.service.entity.TbUserModelConfigDo;
import com.bot.service.service.TbChatConfigDoService;
import com.bot.service.service.TbUserModelConfigDoService;
import com.bot.service.utils.SendMessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Component
public class AuthNode implements NodeAction {

    private final TbUserModelConfigDoService tbUserModelConfigDoService;

    private final TbChatConfigDoService tbChatConfigDoService;

    private final SendMessageUtil sendMessageUtil;
    @Override
    public Map<String, Object> apply(OverAllState state){
        // todo 启动之后校验是否齐全，之后跳过校验
        Long userId = state.value("userId",Long.class).orElse(0L);

        List<TbUserModelConfigDo> list = tbUserModelConfigDoService.getByUserId(userId);
        if (list.isEmpty()) {
            sendMessageUtil.sendChatMsg(userId, "请先配置apikey");
            return Map.of("auth",false);
        }

        TbChatConfigDo one = tbChatConfigDoService.getByUserId(userId);
        if (one == null) {
            TbChatConfigDo tb = new TbChatConfigDo();
            tb.setUserId(userId);
            tb.setTemperature(0.7);
            tb.setTopP(1.0);
            tbChatConfigDoService.save(tb);
        }

        return Map.of("auth",true);
    }
}
