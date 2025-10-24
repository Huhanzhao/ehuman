package com.ai.langchai4j.aihelper.controller;

import com.ai.langchai4j.aihelper.ai.AiServiceInterface;
import com.ai.langchai4j.aihelper.ai.SummaryService;
import com.ai.langchai4j.aihelper.entity.dto.Message;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class AIController {

    @Autowired
    private AiServiceInterface aiServiceInterface;

    @Resource
    private SummaryService summaryService;

    @PostMapping("/chat")
    public String chat(@RequestBody Message message){
        String output = aiServiceInterface.chat(message.getMemoryId(),message.getUserMessage());
        return output;
    }

    @GetMapping("/test/{message}")
    public String chatTest(@PathVariable(name = "message") String message){
        String output = summaryService.summarize(message);
        return output;
    }
}
