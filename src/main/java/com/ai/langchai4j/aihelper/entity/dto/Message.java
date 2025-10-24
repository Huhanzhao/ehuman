package com.ai.langchai4j.aihelper.entity.dto;

import lombok.Data;

@Data
public class Message {
    private String userMessage;
    private String memoryId;
    // todo 后续添加url图片
}
