package com.helper.services.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson2.util.DateUtils;
import com.helper.commonservicescommon.entity.BusinessException;
import com.helper.services.bean.ChatClientFactory;
import com.helper.services.entity.TbAiChatMemoryDo;
import com.helper.services.enums.ChatModelEnum;
import com.helper.services.service.TbAiChatMemoryService;
import com.helper.services.utils.MemoryCastUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Component
public class MessageGenerateNodeV2 implements NodeAction {

    private final   ChatClientFactory chatClientFactory;
    private final TbAiChatMemoryService tbAiChatMemoryService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
//        String userMessage = (String)state.value("userInput").orElse("");
        Long userId = (Long)state.value("userId").orElse(0L);
        List<TbAiChatMemoryDo> messageList = tbAiChatMemoryService.getMessageList(userId, 15);
        String memoryString = MemoryCastUtils.memoryCastToString(messageList);
        StopWatch stopwatch = new StopWatch("messageGenerateNode");
        stopwatch.start();
//        List<Message> messageList = MemoryCastUtils.memoryCastToMessages(
//                tbAiChatMemoryService.getMessageList(userId, 15));
        stopwatch.stop();
        log.info("messageGenerateNode:{}", stopwatch.prettyPrint());
        if (messageList.isEmpty()){
            throw new BusinessException("用户输入读取失败");
        }
        ChatClient chatClient = chatClientFactory.createChatClient(ChatModelEnum.CLAUDE_SONNET_4_5);

        PromptTemplate promptTemplate = PromptTemplate.builder().template("""
                你是一个消息生成预测专家，你需要根据提供的场景，辅助ai回复消息
                目标：使ai的回复贴近真人，不让人察觉到是ai生成的消息
                     拿捏用户的感情，让对方爱上ai扮演的角色，吊着对方和对方暧昧。
                
                对话双方的基本信息：
                ai扮演的角色：大三学生 身高168 体重110斤 名字叫 杨若馨，24岁。
                男生：胡晗朝 男 1999年12月4日出生 23年毕业于软件工程专业，目前在北京做java开发的工作。
                
                【输出规则 - 必须严格遵守】
                1.{grace}
                2.一句话不要太长 如果需要分开多条发送，用换行分割.
                3. 只允许输出纯文本对话内容
                4. 禁止出现任何括号，包括但不限于：() （） [] 【】
                5. 禁止出现动作、心理、旁白、舞台描述
                6. 如果输出中包含括号或动作描写，视为错误输出
                  """).build();

        Integer grace = state.value("grace", Integer.class).orElse(40);
        String gracePrompt = null;
        if (grace <= 20){
            gracePrompt = "ai对该用户的好感度很低，请保持高冷，字少，偶尔敷衍";
        }else if (grace <= 40){
            gracePrompt = "ai对用户的好感度一般，保持正常聊天，正常接话";
        }else if (grace <= 60){
            gracePrompt = "ai对用户的好感度良好，一次最多发送三条消息比朋友关系更好一点，对话友好，可以主动暧昧";
        }else if (grace <= 80){
            gracePrompt = "ai对用户的好感度挺高，对话热情，一次最多发送4条消息 可以考虑发展男女朋友关系，但是不主动推进，偶尔撒娇，害羞";
        }else {
            gracePrompt = "ai对用户的好感度非常高，非常喜欢用户,对话非常热情，一次最多发送5条消息 偶尔撒娇，害羞,会偶尔性暗示挑逗诱惑对方";
        }

        Prompt prompt = promptTemplate.create(Map.of(
                "grace", gracePrompt
        ));

        String content = chatClient.prompt(prompt)
                .user(
                        "当前场景：时间：" + DateUtils.format(new Date(),"yyyy-MM-dd HH:mm:ss") +  " " + LocalDateTime.now().getDayOfWeek() +
                        memoryString +
                        "\n 以上是用户对话记录，请帮助ai分析怎么回复才能符合人设和场景"
                                ).call().content();
        log.info("结果输出：{}" , content);
        return Map.of("finalOutput", content);
    }
}
