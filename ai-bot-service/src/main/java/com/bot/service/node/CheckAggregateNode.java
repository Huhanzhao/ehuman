package com.bot.service.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class CheckAggregateNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        Integer reTry = (Integer)state.value("reTry").orElse(0);
        String memoryCheckOutput = (String) state.value("memoryCheckOutput").orElse("");
        String roleReversalCheckOutput = (String) state.value("roleReversalCheckOutput").orElse("");

        boolean checkPass = true;
        if (!"通过".equals(memoryCheckOutput.trim())) {
            log.info("回复超出记忆，开始进行异常处理");
            state.input(Map.of("failReason", memoryCheckOutput + "\n"));
            checkPass = false;
        }

        if (!"通过".equals(roleReversalCheckOutput.trim())) {
            log.info("角色混乱异常，开始进行异常处理");
            state.input(Map.of("failReason", roleReversalCheckOutput + "\n"));
            checkPass = false;
        }

        return Map.of("reTry", reTry + 1, "checkPass", checkPass );
    }

    public static String nextNode(OverAllState state) {
        // 路由下一个节点
        Boolean checkPass = (Boolean) state.value("checkPass").orElse(true);
        if (!checkPass) {
            return "messageGenerateNode";
        }
        return "transformTagMessageNode";
    }

}
