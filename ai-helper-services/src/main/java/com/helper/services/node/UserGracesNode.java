package com.helper.services.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.helper.services.bean.ChatClientFactory;
import com.helper.services.entity.TbUserGraceDo;
import com.helper.services.service.TbUserGraceDoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserGracesNode implements NodeAction {

    private final ChatClientFactory chatClientFactory;

    private final TbUserGraceDoService tbUserGraceDoService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        Long userId = state.value("userId",Long.class).orElse(0L);

        if (userId == 0){
            log.error("用户ID为空");
            return Map.of("graces", "unKnow");
        }
        TbUserGraceDo graceDo = tbUserGraceDoService.getOne(Wrappers.<TbUserGraceDo>lambdaQuery().eq(TbUserGraceDo::getUserId, userId));
        if (graceDo == null) {
            graceDo = new TbUserGraceDo().setUserId((long)userId).setGrace(0);
            tbUserGraceDoService.save(graceDo);
        }
        log.info("用户好感度:{}",graceDo.getGrace());
//        ChatClient chatClient = chatClientFactory.createChatClient(ChatModelEnum.GPT_4O_MINI);
//        String content = chatClient.prompt().messages().call().content();  // todo 根据对话判断是否要修改好感度
        return Map.of("graces", "好感度：" + graceDo.getGrace() + "(范围 -100~100)");
    }

    public static String decideNextNode(OverAllState state) {
        // 你可以在这里使用 `state` 来判断条件
        Integer someCondition = (Integer) state.value("input").orElse(0);

        if (someCondition > 3) {
            return "bbb";  // 如果条件满足，返回 node1
        } else {
            return "aaa";  // 否则返回 node3
        }
    }
}
