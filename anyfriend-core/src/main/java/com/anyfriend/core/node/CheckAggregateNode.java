package com.anyfriend.core.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.anyfriend.dao.service.TbUserFeatureButtonDoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.anyfriend.dao.enums.FeatureTypeEnum.VOICE_PLUGIN;

@RequiredArgsConstructor
@Component
@Slf4j
public class CheckAggregateNode implements NodeAction {

    private final TbUserFeatureButtonDoService tbUserFeatureButtonDoService;

    private static final Pattern CHECK_LINE_PATTERN = Pattern.compile("(记忆编造审核|角色混乱审核|现实互动审核|时间混淆审核)\\s*[:：]\\s*(不通过)\\s*(.*)");

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
        resultMap.put("checkPass", checkPass);
        resultMap.put("reTry", reTry + 1);
        if (!checkPass) {
            resultMap.put("failReason", String.join("\n", failReasons));
        }

        if (reTry > 3) {
            log.error("消息生成出现{}轮重试，放弃", reTry);
            resultMap.put("checkPass", true);
        }

        return resultMap;
    }

    public String nextNode(OverAllState state) {
        Boolean checkPass = (Boolean) state.value("checkPass").orElse(true);
        Long userId = state.value("userId",Long.class).orElse(0L);
        Boolean initiative = state.value("initiative",Boolean.class).orElse(false);
        if (!checkPass) {
            if (initiative){
                return "initiativeFail";
            }
            return "fail";
        }

        // 开头结尾格式判断，如果符合直接发送
        boolean needFormat = false;
        String generateMessage = state.value("generateMessage",String.class).orElse("");
        boolean enableVoice = tbUserFeatureButtonDoService.isEnableByUserId(userId, VOICE_PLUGIN);
        String[] split = generateMessage.trim().split("\n");

        List<Predicate<String>> predicates = new ArrayList<>();
        predicates.add(s -> s.trim().startsWith("<text>") && s.trim().endsWith("</text>"));
        predicates.add(s -> s.trim().startsWith("<silence>") && s.trim().endsWith("</silence>"));
        predicates.add(s -> s.trim().startsWith("<emoji emo=\"") && s.trim().endsWith("/>"));
        predicates.add(s -> s.trim().isEmpty());
        if (enableVoice){
            // 开启了语音
            predicates.add(s -> s.trim().startsWith("<voice") && s.trim().endsWith("</voice>"));
        }
        for (String segment : split) {  // 遍历每个片段
            // 判断当前片段是否匹配 predicates 里的任意一个
            boolean isValid = predicates.stream().anyMatch(p -> p.test(segment.trim()));

            if (!isValid) {
                needFormat = true;
                break;
            }
            if (segment.trim().contains("(")
                    || segment.trim().contains("[")
                    || segment.trim().contains("{")
                    || segment.trim().contains(")")
                    || segment.trim().contains("]")
                    || segment.trim().contains("}")
                    || segment.trim().contains("）")
                    || segment.trim().contains("（")
                    || segment.trim().contains("\n")
            ){
                needFormat = true;
                break;
            }
        }
        return needFormat? "format" : "pass";
    }
}
