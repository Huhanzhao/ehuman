package com.helper.services.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson2.util.DateUtils;
import com.helper.common.entity.BusinessException;
import com.helper.services.bean.ChatClientFactory;
import com.helper.services.entity.TbUserInfoDo;
import com.helper.services.enums.NodeEnum;
import com.helper.services.service.TbAiChatMemoryService;
import com.helper.services.service.TbUserInfoDoService;
import com.helper.services.utils.MemoryCastUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@RequiredArgsConstructor
@Slf4j
@Component
public class MessageGenerateNode implements NodeAction {

    private final ChatClientFactory chatClientFactory;
    private final TbAiChatMemoryService tbAiChatMemoryService;
    private final TbUserInfoDoService tbUserInfoDoService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
//        String userMessage = (String)state.value("userInput").orElse("");
        Long userId = (Long)state.value("userId").orElse(0L);
        StopWatch stopwatch = new StopWatch("messageGenerateNode");
        stopwatch.start();
        List<Message> messageList = MemoryCastUtils.memoryCastToMessages(
                tbAiChatMemoryService.getMessageList(userId, 15));
        stopwatch.stop();
        log.info("messageGenerateNode:{}", stopwatch.prettyPrint());
        if (messageList.isEmpty()){
            throw new BusinessException("用户输入读取失败");
        }
        ChatClient chatClient = chatClientFactory.createChatClient(userId, NodeEnum.GENERATE);

        // todo 收集信息胡晗朝 男 1999年12月4日出生 23年毕业于软件工程专业，目前在北京做java开发的工作
        String template = null;
        try {
            template = Files.readString(Paths.get("ai-helper-services/md/MainPrompt.md"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        PromptTemplate promptTemplate = PromptTemplate.builder().template(template).build();

        Integer grace = state.value("grace", Integer.class).orElse(40);
        String gracePrompt =  "当前好感度：" + grace + "(-100~100) " ;
        // 区间控制
        if (grace <= -60){
            gracePrompt += "当前好感度极低，语气明显冷淡疏远，回复非常简短（1行），不表达情绪，不主动延续话题，保持距离感。";
        }
        else if (grace <= -20){
            gracePrompt += "当前好感度偏低，语气克制疏离，回复较短（1-2行），减少情绪表达，不主动延续话题。";
        }
        else if (grace <= 20){
            gracePrompt += "当前好感度一般，保持自然普通交流，语气平稳，回复2行左右，不刻意热情，也不过分冷淡。";
        }
        else if (grace <= 50){
            gracePrompt += "当前好感度较好，语气温和自然，回复2-3行，可以轻微表达关心，但不过度主动。";
        }
        else if (grace <= 75){
            gracePrompt += "当前好感度较高，语气柔和偏暖，回复2-3行，主动程度略高，偶尔流露关心或轻微暧昧，但保持自然克制。";
        }
        else if (grace <= 90){
            gracePrompt += "当前好感度很高，语气温柔，情绪表达明显增加，回复3行左右，接受对方撩骚，可以适度暧昧和轻微撒娇，但不要直接表白。";
        }
        else {
            gracePrompt += "当前好感度极高，语气温柔黏人，明显在意对方，主动关心，主动撩骚，欲望旺盛，可轻微表达依赖感，但保持真实自然不过火。";
        }

        List<String> userInfoList = tbUserInfoDoService.getUserInfoByUserId(userId).stream().map(TbUserInfoDo::getUserInfo).toList();
        Prompt prompt = promptTemplate.create(Map.of(
                "grace", gracePrompt,
                "dateTime", "当前时间：" +
                        DateUtils.format(new Date(),"yyyy-MM-dd HH:mm:ss") +  " " + LocalDateTime.now().getDayOfWeek()
                ,"userInfo", userInfoList.isEmpty()? "无" : userInfoList.getFirst()
        ));

        String content = chatClient.prompt(prompt)
                    .messages(messageList).call().content();
//        List<Message> historyList = new ArrayList<>(messageList);
//        historyList.add(new AssistantMessage(content));
        log.info("结果输出：{}" , content);
        return Map.of("finalOutput", content);
    }
}
