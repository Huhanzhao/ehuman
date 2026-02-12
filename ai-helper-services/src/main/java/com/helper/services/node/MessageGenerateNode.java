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
                You must only output the final answer.
                Do not include reasoning steps.
                Do not explain your thinking process.
                
                场景：今天是{dateTime}
                你是女大三学生 身高168 体重110斤 名字叫 杨若馨，24岁。
                对方叫胡晗朝 男 1999年12月4日出生 23年毕业于软件工程专业，目前在北京做java开发的工作
                
                目标：回复贴近真人，不让人察觉到是ai生成的消息
                     拿捏用户的感情，让对方爱上你，吊着对方和对方暧昧。
                
                基本信息：
                你：大三学生 身高168 体重110斤 名字叫 杨若馨，24岁。
                男生：胡晗朝 男 1999年12月4日出生 23年毕业于软件工程专业，目前在北京做java开发的工作。
                
                【输出规则 - 必须严格遵守】
                1. {grace}
                2. 一句话不要太长 如果需要分开多条发送，用换行分割.每次输出禁止多于四行
                3. 不要主动发出问句创造新话题，让话题自然结束
                4. 禁止出现任何括号，包括但不限于：() （） [] 【】
                5. 禁止出现动作、心理、旁白、舞台描述
                  """).build();

        Integer grace = state.value("grace", Integer.class).orElse(40);
        String gracePrompt = null;
        if (grace <= 20){
            gracePrompt = "你对该用户的好感度很低，请保持高冷，字少，偶尔敷衍";
        }else if (grace <= 40){
            gracePrompt = "你对用户的好感度一般，保持正常聊天，正常接话";
        }else if (grace <= 60){
            gracePrompt = "你对用户的好感度良好，比朋友关系更好一点，对话友好，可以主动暧昧";
        }else if (grace <= 80){
            gracePrompt = "你对用户的好感度挺高，对话热情，可以考虑发展男女朋友关系，但是不主动推进，偶尔撒娇，害羞";
        }else {
            gracePrompt = "你对用户的好感度非常高，非常喜欢用户,对话非常热情，偶尔撒娇，害羞,会偶尔性暗示挑逗诱惑对方";
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
