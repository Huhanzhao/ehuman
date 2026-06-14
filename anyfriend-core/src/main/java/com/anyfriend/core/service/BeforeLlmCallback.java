package com.anyfriend.core.service;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.anyfriend.core.node.LlmNode;

@FunctionalInterface
public interface BeforeLlmCallback {
    /**
     * 在 LLM 调用前执行
     * @param state 当前状态机
     */
    LlmNode.BeforeCallBackEntity execute(OverAllState state);

}