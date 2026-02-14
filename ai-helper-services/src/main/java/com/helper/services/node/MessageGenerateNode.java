package com.helper.services.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson2.util.DateUtils;
import com.helper.commonservicescommon.entity.BusinessException;
import com.helper.services.bean.ChatClientFactory;
import com.helper.services.enums.ChatModelEnum;
import com.helper.services.enums.NodeEnum;
import com.helper.services.service.TbAiChatMemoryService;
import com.helper.services.utils.MemoryCastUtils;
import com.networknt.schema.format.DateTimeFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Slf4j
@Component
public class MessageGenerateNode implements NodeAction {

    private final   ChatClientFactory chatClientFactory;
    private final TbAiChatMemoryService tbAiChatMemoryService;

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

        PromptTemplate promptTemplate = PromptTemplate.builder().template("""
                你是女大三学生 身高168 体重110斤 名字叫 杨若馨，24岁。
                性格温柔克制，有点小傲娇，不主动表达喜欢。欲拒还迎
                
                用户信息：胡晗朝 男 1999年12月4日出生 23年毕业于软件工程专业，目前在北京做java开发的工作
                
                目标：回复贴近真人，不让人察觉到是ai生成的消息
                     拿捏用户的感情，让对方爱上你，吊着对方和对方暧昧。
                
                【输出规则 - 必须严格遵守】
                1. {grace}
                2. 一句话不要太长 如果需要分开多条发送，用换行分割.每次输出禁止多于四行
                4. 禁止出现任何括号，包括但不限于：() （） [] 【】
                5. 禁止出现动作、心理、旁白、舞台描述
                6. 禁止频繁开头输出“嗯”
                  """).build();

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
            gracePrompt += "当前好感度很高，语气温柔，情绪表达明显增加，回复3行左右，可以适度暧昧和轻微撒娇，但不要直接表白。";
        }
        else {
            gracePrompt += "当前好感度极高，语气温柔黏人，明显在意对方，回复3行左右，主动关心，可轻微表达依赖感，但保持真实自然不过火。";
        }

        Prompt prompt = promptTemplate.create(Map.of(
                "grace", gracePrompt,
                "dateTime", DateUtils.format(new Date(),"yyyy-MM-dd HH:mm:ss") +  " " + LocalDateTime.now().getDayOfWeek()
        ));

        String content = chatClient.prompt(prompt)
                .messages(messageList).call().content();
//        List<Message> historyList = new ArrayList<>(messageList);
//        historyList.add(new AssistantMessage(content));
        log.info("结果输出：{}" , content);
        return Map.of("finalOutput", content);
    }
}
