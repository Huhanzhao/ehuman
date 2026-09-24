package com.ehuman.core.service;

import com.alibaba.cloud.ai.graph.OverAllState;

@FunctionalInterface
public interface AfterLlmCallback {
    /**
     * 在 LLM 调用后执行
     * @param state 当前状态机
     * @param llmOutput LLM 的输出结果
     */
    void execute(OverAllState state, String llmOutput);
}