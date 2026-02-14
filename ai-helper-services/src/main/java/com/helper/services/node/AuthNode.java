package com.helper.services.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.helper.services.entity.TbChatConfigDo;
import com.helper.services.entity.TbUserModelConfigDo;
import com.helper.services.service.TbChatConfigDoService;
import com.helper.services.service.TbUserModelConfigDoService;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
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

    private final BotContainer botContainer;
    @Override
    public Map<String, Object> apply(OverAllState state){
        Long userId = state.value("userId",Long.class).orElse(0L);

        List<TbUserModelConfigDo> list = tbUserModelConfigDoService.list(Wrappers.<TbUserModelConfigDo>lambdaQuery()
                .eq(TbUserModelConfigDo::getUserId, userId));
        if (list.isEmpty()) {
            Bot bot = botContainer.robots.get(3839330250L);
            // 调用 Bot 对象方法
            bot.sendPrivateMsg(userId, "请先配置apikey", false);
            return Map.of("auth",false);
        }

        TbChatConfigDo one = tbChatConfigDoService.getOne(Wrappers.<TbChatConfigDo>lambdaQuery()
                .eq(TbChatConfigDo::getUserId, userId));
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
