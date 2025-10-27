package com.ai.langchai4j.aihelper.controller;

import com.ai.langchai4j.aihelper.ai.AICodeService;
import com.ai.langchai4j.aihelper.ai.AIServiceTest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/aiCode")
public class AICodeController {

    @Resource
    private AICodeService aiCodeService;

    @Resource
    private AIServiceTest aiServiceTest;

    @RequestMapping("/chat")
    public String chat(@RequestParam String message){
        String s = aiCodeService.chatWithString("你好，介绍下自己");
        return "hello world";
    }

    // 测试流式输出
    @GetMapping(value = "/chat/{message}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@PathVariable(name = "message") String message){

        SseEmitter emitter = new SseEmitter();

        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
                .apiKey("sk-PLLvztGg0Oa2qxQ5TfkMuU4j7PT5MbUm6Mo7Kaei21HQfffo")
                .modelName("gpt-4o-mini")
                .baseUrl("https://api.openai-proxy.org/v1")
                .build();
        model.chat("你好，介绍下自己", new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                try {
                    emitter.send(SseEmitter.event().data(partialResponse));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse chatResponse) {
                emitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                emitter.completeWithError(error);
            }
        });
        return emitter;
    }

    //aiservice 测试
    @GetMapping("/chatWithMessage/{name}/{message}")
    public String chatWithMessage(@PathVariable (name = "message") String message,@PathVariable (name = "name") String name){
        return aiServiceTest.chatWithString(name,message);
    }
}
