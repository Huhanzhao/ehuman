package com.ai.langchai4j.aihelper.controller;

import com.ai.langchai4j.aihelper.ai.AICodeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/aiCode")
public class AICodeController {

    @Resource
    private AICodeService aiCodeService;

    @RequestMapping("/chat")
    public String chat(@RequestParam String message){
        String s = aiCodeService.chatWithString("你好，介绍下自己");
        return "hello world";
    }

}
