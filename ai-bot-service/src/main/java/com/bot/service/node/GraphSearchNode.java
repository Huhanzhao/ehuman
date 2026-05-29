package com.bot.service.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.bot.service.bean.ChatClientFactory;
import com.bot.service.service.TbBotLifeDetailDoService;
import com.bot.service.service.TbUserBotStatusDoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GraphSearchNode implements NodeAction {

    private final ChatClientFactory chatClientFactory;
    private final TbUserBotStatusDoService tbUserBotStatusDoService;
    private final TbBotLifeDetailDoService tbBotLifeDetailDoService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("开始检索图库");
        // todo 搜索记忆
        return Map.of();
    }
}
