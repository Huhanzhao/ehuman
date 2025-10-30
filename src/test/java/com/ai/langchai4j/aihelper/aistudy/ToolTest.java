package com.ai.langchai4j.aihelper.aistudy;

import com.ai.langchai4j.aihelper.aistudy.tools.SearchWeather;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ToolTest {

    @Autowired
    private OpenAiChatModel openAiChatModel;

    @Test
    void searchWeather() {
        AIServiceTest aiServiceTest = AiServices.builder(AIServiceTest.class)
                .tools(new SearchWeather())
                .chatModel(openAiChatModel)
                .build();

//        String s = aiServiceTest.chatWithString("上海是什么天气");
        String s = aiServiceTest.chatWithString("1/1等于几");

        System.out.println(s);
    }
}
