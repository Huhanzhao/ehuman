package com.helper.services.controller;


import com.helper.services.entity.dto.ChatRequest;
import com.helper.services.service.AIChatService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RequestMapping("/ai")
@RestController
// ai对话接口
public class AIController {

    // 聊天服务
    private final AIChatService aiChatService;

    // RAG服务
    private final AIChatService aiRagService;


    public AIController(@Qualifier("getAIChatService") AIChatService aiChatService,
                        @Qualifier("getAIRagService") AIChatService aiRagService) {
        this.aiChatService = aiChatService;
        this.aiRagService = aiRagService;
    }

    // 简单对话
    @PostMapping(value = "/simpleChat", produces = "text/plain;charset=UTF-8")
    public Flux<String> chatWithString(@RequestBody ChatRequest chatRequest){
        return aiChatService.chatWithString("1111",chatRequest.getMessage());
    }

    // RAG旧记忆 + RAG资料库 + 联网检索 + 图片生成

    // 图片上传接口

}
