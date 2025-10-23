package com.ai.langchai4j.aihelper.controller;

import com.ai.langchai4j.aihelper.ai.AiServiceInterface;
import com.ai.langchai4j.aihelper.entity.dto.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class AIController {

    @Autowired
    private AiServiceInterface aiServiceInterface;

    @PostMapping("/chat")
    public String chat(@RequestBody Message message){
        String output = aiServiceInterface.chat(message.getUserMessage());
        return output;
    }
}
