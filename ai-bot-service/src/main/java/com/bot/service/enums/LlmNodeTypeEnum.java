package com.bot.service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: huhanzhao
 * @CreateTime: 2026-06-11
 * @Description: LlmNode类型枚举
 */

@Getter
@AllArgsConstructor
public enum LlmNodeTypeEnum {

    MEMORY_CHECK("memoryCheck", "./md/MemoryCheckPrompt.md", "memoryCheckOutput"),
    INTERACTION_CHECK("interactionCheckOutput", "./md/InteractionCheckPrompt.md", "interactionCheckOutput");

    private final String nodeName;
    private final String promptTemplatePath;
    private final String outputKey;
}
