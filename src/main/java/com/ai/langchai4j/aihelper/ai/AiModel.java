package com.ai.langchai4j.aihelper.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AiModel {

    @Autowired
    private ChatModel openAiChatModel;

    private static final String systemPrompt = """
            你是编程领域的小助手，帮助用户解答编程学习和求职面试相关的问题，并给出建议。重点关注 4 个方向：
             1.规划清晰的编程学习路线
             2.提供项目学习建议
             3.给出程序员求职全流程指南（比如简历优化、投递技巧）
             4.分享高频面试题和面试技巧
             5.请用简洁易懂的语言回答，助力用户高效学习与求职。
            """;

    public String chatWithModel(String input){
        UserMessage userMessage = UserMessage.from(input);
        ChatResponse chatResponse = openAiChatModel.chat(userMessage);
        AiMessage aiMessage = chatResponse.aiMessage();
        log.info(aiMessage.toString());
        return aiMessage.text();
    }

    public String chatWithModelByMessages(String input){
        UserMessage userMessage = UserMessage.from(input);
        SystemMessage systemMessage = SystemMessage.from(systemPrompt);
        ChatResponse chatResponse = openAiChatModel.chat(systemMessage,userMessage);
        AiMessage aiMessage = chatResponse.aiMessage();
        log.info(aiMessage.toString());
        return aiMessage.text();
    }
}
