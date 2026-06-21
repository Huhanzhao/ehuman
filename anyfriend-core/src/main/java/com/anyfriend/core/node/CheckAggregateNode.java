package com.anyfriend.core.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Component
@Slf4j
public class CheckAggregateNode implements NodeAction {

    private static final Pattern CHECK_LINE_PATTERN = Pattern.compile("(记忆编造|角色混乱|现实互动)\\s*[:：]\\s*(不通过)\\s*(.*)");

    @Override
    public Map<String, Object> apply(OverAllState state) {
        Integer reTry = (Integer) state.value("reTry").orElse(0);
        String combinedCheckOutput = (String) state.value("combinedCheckOutput").orElse("");

        Map<String, Object> resultMap = new HashMap<>();
        List<String> failReasons = new ArrayList<>();

        Matcher matcher = CHECK_LINE_PATTERN.matcher(combinedCheckOutput);
        while (matcher.find()) {
            String checkName = matcher.group(1);
            String reason = matcher.group(3).trim();
            failReasons.add(checkName + "：" + reason);
            log.info("审核不通过 - {} {}", checkName, reason);
        }

        boolean checkPass = failReasons.isEmpty();
        if (!checkPass) {
            resultMap.put("failReason", String.join("\n", failReasons));
        }

        resultMap.put("checkPass", checkPass);
        resultMap.put("reTry", reTry + 1);

        if (reTry > 3) {
            log.error("消息生成出现{}轮重试，放弃", reTry);
            resultMap.put("checkPass", true);
        }

        return resultMap;
    }

    public static String nextNode(OverAllState state) {
        Boolean checkPass = (Boolean) state.value("checkPass").orElse(true);
        if (!checkPass) {
            return "messageGenerateNode";
        }
        return "transformTagMessageNode";
    }
}
