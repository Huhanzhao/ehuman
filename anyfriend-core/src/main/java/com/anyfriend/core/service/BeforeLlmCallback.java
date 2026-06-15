package com.anyfriend.core.service;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

@FunctionalInterface
public interface BeforeLlmCallback {
    /**
     * 在 LLM 调用前执行
     * @param state 当前状态机
     */
    BeforeCallBackEntity execute(OverAllState state);


    record BeforeCallBackEntity(List<Message> messages, String userMessage, String interruptionMessage) {}
}