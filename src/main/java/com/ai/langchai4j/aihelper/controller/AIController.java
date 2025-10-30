package com.ai.langchai4j.aihelper.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RequestMapping("/ai")
@RestController
// ai对话接口
public class AIController {

    // 简单对话
    @GetMapping("/simpleChat")
    public Flux<String> chatWithString(String message){
        return null;
    }

    // RAG旧记忆 + RAG资料库 + 联网检索 + 图片生成

    // 图片上传接口

}
