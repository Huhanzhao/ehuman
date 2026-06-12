package com.bot.service.service;

import com.alibaba.cloud.ai.graph.OverAllState;

import java.util.Map;

@FunctionalInterface
public interface BeforeLlmCallback {
    /**
     * 在 LLM 调用前执行
     * @param state 当前状态机
     */
    void execute(OverAllState state);
}