package com.ai.langchai4j.aihelper.ai;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static dev.langchain4j.model.openai.OpenAiImageModelName.DALL_E_3;

@SpringBootTest
class AICodeServiceImplTest {

    @Autowired
    private AICodeService aiCodeService;

//    @Autowired
//    private StreamingChatModel streamingChatModel;

    @Autowired
    private OpenAiImageModel openAiImageModel;

    @Test
    void chatWithMessage() {
        List<ChatMessage> messageList = new ArrayList<>(List.of(
                SystemMessage.from("你是一个助手，请按照下面的格式回答：\n" +
                        "1. 你好，我是xxx\n" +
                        "2. 你喜欢吃什么\n" +
                        "3. 你喜欢吃什么\n" +
                        "4. 你喜欢吃什么\n" +
                        "5. 你喜欢吃什么\n" +
                        "6. 你喜欢吃什么\n" +
                        "7. 你喜欢吃什么\n"
                ), UserMessage.from("你是谁"), AiMessage.from("你好我是你的助手")
        ));

        messageList.add(UserMessage.from("我上面问的你什么问题？"));


        ChatResponse chatResponse = aiCodeService.chatWithMessage(messageList);
        System.out.println(chatResponse.id());
        System.out.println(chatResponse.aiMessage().text());
    }

    @Test
    void chatWithString() {
        String s = aiCodeService.chatWithString("你好，介绍下自己");
        System.out.println(s);
    }

    @Test
    void chatWithRequest() {
        List<ChatMessage> messageList = new ArrayList<>(List.of(
                SystemMessage.from("你是一个助手"),
                UserMessage.from("你是谁"),
                AiMessage.from("你好我是你的助手")
        ));
        messageList.add(UserMessage.from("你叫什么名字呢？"));
        ChatResponse chatResponse = aiCodeService.chatWithRequest(messageList);
        System.out.println(chatResponse.id());
        System.out.println(chatResponse.aiMessage().text());
    }

    @Test
    void chatWithImageAndAudio() {
        ChatResponse chatResponse = aiCodeService.chatWithImageAndAudio("描述一下下面这张图片", "https://gw.alipayobjects.com/zos/rmsportal/BiazfanxmamNRoxxVxka.png");
        System.out.println(chatResponse.id());
        System.out.println(chatResponse.aiMessage().text());
    }

    @Test
    void chatWithMemoryMessages() {
        List<ChatMessage> messageList = new ArrayList<>(List.of(
                SystemMessage.from("你是一个助手"),
                UserMessage.from("我叫小明"),
                AiMessage.from("你好我是你的助手")
        ));
        messageList.add(UserMessage.from("我叫什么名字？"));
        String chatResponse = aiCodeService.chatWithMemoryMessages(messageList);
        System.out.println(chatResponse);
    }

    @Test
    void chatWithMemoryMessagesPersistence() {
        String chatResponse = aiCodeService.chatWithMemoryMessagesPersistence(List.of(UserMessage.from("我叫什么名字")));
        System.out.println(chatResponse);
    }

    // 图片生成
    @Test
    void openAiImageModel() {
        ImageModel model = OpenAiImageModel.builder()
                .apiKey("sk-PLLvztGg0Oa2qxQ5TfkMuU4j7PT5MbUm6Mo7Kaei21HQfffo")
                .baseUrl("https://api.openai-proxy.org/v1")
                .modelName(DALL_E_3)
                .build();

        Response<Image> response = model.generate("一个鸭子");

        System.out.println(response.content().url()); // Donald Duck is here :)
    }

    // 图片生成
    @Test
    void autoImageGenerate() {
        Response<Image> response = openAiImageModel.generate("一个鸭子");

        System.out.println(response.content().url()); // Donald Duck is here :)
    }
}