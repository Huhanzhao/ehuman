package com.ai.langchai4j.aihelper.aistudy;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

@SpringBootTest
class AIServiceTestTest {

    @Autowired
    private OpenAiChatModel openAiChatModel;

    @Test
    void chat() {
        OpenAiStreamingChatModel streamingChatModel = OpenAiStreamingChatModel.builder()
                .apiKey("sk-PLLvztGg0Oa2qxQ5TfkMuU4j7PT5MbUm6Mo7Kaei21HQfffo")
                .modelName("gpt-4o-mini")
                .baseUrl("https://api.openai-proxy.org/v1")
                .build();

        AIServiceTest aiServiceTest = AiServices.create(AIServiceTest.class, streamingChatModel);

        // 这种用不了
        //        AIServiceTest build = AiServices.builder(AIServiceTest.class)
//                .chatModel(streamingChatModel)
//                .build();
        CompletableFuture<ChatResponse> futureResponse = new CompletableFuture<>();

        TokenStream chat = aiServiceTest.chatStream("你好，介绍下自己");
        chat.onPartialResponse((String partialResponse) -> System.out.println(partialResponse))
                .onRetrieved((List<Content> contents) -> System.out.println(contents))
                .onToolExecuted((ToolExecution toolExecution) -> System.out.println(toolExecution))
                .onCompleteResponse((ChatResponse response) -> System.out.println(response))
                .onError((Throwable error) -> error.printStackTrace())
                .start();

        try {
            ChatResponse chatResponse = futureResponse.get(30, SECONDS);
            System.out.println(chatResponse.aiMessage().text());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    void chatStream2() {
        OpenAiStreamingChatModel streamingChatModel = OpenAiStreamingChatModel.builder()
                .apiKey("sk-PLLvztGg0Oa2qxQ5TfkMuU4j7PT5MbUm6Mo7Kaei21HQfffo")
                .modelName("gpt-4o-mini")
                .baseUrl("https://api.openai-proxy.org/v1")
                .build();
        AIServiceTest aiServiceTest = AiServices.create(AIServiceTest.class, streamingChatModel);
        Flux<String> stringFlux = aiServiceTest.chatStream2("你好，介绍下自己");
        // 阻塞方式，适合 main() 测试
        stringFlux.doOnNext(System.out::println)
                .blockLast(); // 等待 Flux 完成
//        stringFlux.subscribe(System.out::println);
    }
}