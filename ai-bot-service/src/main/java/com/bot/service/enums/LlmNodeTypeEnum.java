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
    FORMAT_OUTPUT("formatOutPut", "./md/FormatOutPutPrompt.md", "formatOutPut"),
    ROLE_REVERSAL_CHECK("roleReversalCheck", "./md/RoleReversalCheckPrompt.md", "roleReversalCheckOutput"),
    INTERACTION_CHECK("interactionCheckOutput", "./md/InteractionCheckPrompt.md", "interactionCheckOutput");

    private final String nodeName;
    private final String promptTemplatePath;
    private final String outputKey;
}
