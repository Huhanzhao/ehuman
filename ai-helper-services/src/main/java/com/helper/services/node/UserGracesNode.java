package com.helper.services.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson2.JSON;
import com.alibaba.nacos.shaded.io.grpc.internal.JsonUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.helper.services.bean.ChatClientFactory;
import com.helper.services.entity.TbAiChatMemoryDo;
import com.helper.services.entity.TbUpdateGraceHistoryDo;
import com.helper.services.entity.TbUserGraceDo;
import com.helper.services.enums.ChatModelEnum;
import com.helper.services.service.TbAiChatMemoryService;
import com.helper.services.service.TbUpdateGraceHistoryDoService;
import com.helper.services.service.TbUserGraceDoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserGracesNode implements NodeAction {

    private final ChatClientFactory chatClientFactory;

    private final TbUserGraceDoService tbUserGraceDoService;

    private final TbUpdateGraceHistoryDoService tbUpdateGraceHistoryDoService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        Long userId = state.value("userId",Long.class).orElse(0L);
        String memoryString = state.value("memoryString",String.class).orElse("");

        if (userId == 0){
            log.error("用户ID为空");
            return Map.of("graces", "unKnow");
        }

        ChatClient chatClient = chatClientFactory.createChatClient(ChatModelEnum.CLAUDE_SONNET_4_5);
        String content = chatClient.prompt(
                """
                        你是一个好感度处理模块，你需要根据对话来分析ai角色是否需要对用户好感度进行修改。
                        判断规则如下：
                        系统好感度范围是：-100~100
                        1.减少好感度条件:
                          * 用户对话会让角色感觉到冒犯
                          * 用户发脾气，辱骂等
                        2.增加好感度条件：
                          * 用户对话让角色感觉到温馨，用户关心等
                          * 用户对话
                        如果有其他情况你觉得需要减分或者加分的也可以自行决定
                        
                        严格按照json格式输出结果：
                        例如：{"increase": 10,"decrease": 0,"reason": "感觉到关心"} 
                        代表增加10个好感度 原因是感觉到了关心，不同情况请简短的总结一个原因
                        只需要给我json,不用解释
                        """
        ).user(memoryString + "\n 以上是用户与ai对话历史，请分析用户对ai的好感度").call().content();

        // 查询用户好感度
        TbUserGraceDo graceDo = tbUserGraceDoService.getOne(Wrappers.<TbUserGraceDo>lambdaQuery().eq(TbUserGraceDo::getUserId, userId));
        if (graceDo == null) {
            graceDo = new TbUserGraceDo().setUserId((long)userId).setGrace(0);
            tbUserGraceDoService.save(graceDo);
        }

        Integer grace = graceDo.getGrace();
        // 修改好感度
        try {
            content = content.replaceAll("```json","").replaceAll("```","");
            TbUpdateGraceHistoryDo parse = JSON.parseObject(content,TbUpdateGraceHistoryDo.class);
            parse.setUserId(userId);
            tbUpdateGraceHistoryDoService.save(parse);

            if (parse.getIncrease() != null && parse.getIncrease() > 0){
                grace += parse.getIncrease();
            }else if (parse.getDecrease() != null && parse.getDecrease() > 0){
                grace -= parse.getDecrease();
            }
            grace = grace < 100 ? grace : 100;
            grace = grace > -100 ? grace : -100;
            tbUserGraceDoService.update(Wrappers.<TbUserGraceDo>lambdaUpdate()
                    .set(TbUserGraceDo::getGrace, grace)
                    .eq(TbUserGraceDo::getUserId, userId));
        }catch (Exception e){
            log.error("解析用户好感度失败:{}",e.getMessage());
        }

        log.info("用户好感度:{}",graceDo.getGrace());
        return Map.of("grace", grace);
    }
}
