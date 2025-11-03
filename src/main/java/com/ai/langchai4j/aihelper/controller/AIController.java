package com.ai.langchai4j.aihelper.controller;


import com.ai.langchai4j.aihelper.demand.service.AIChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RequestMapping("/ai")
@RestController
// ai对话接口
public class AIController {

    @Autowired
    private AIChatService aiChatService;

    // 简单对话
    @GetMapping(value = "/simpleChat/{message}", produces = "text/plain;charset=UTF-8")
    public Flux<String> chatWithString(@PathVariable(name = "message") String message){
        return aiChatService.chatWithString(message);
    }

    // RAG旧记忆 + RAG资料库 + 联网检索 + 图片生成

    // 图片上传接口

}
