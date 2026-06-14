package com.anyfriend.dao.enums;

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

    MEMORY_CHECK("memoryCheck", "./md/MemoryCheckPrompt.md", "memoryCheckOutput",0),
    FORMAT_OUTPUT("formatOutPut", "./md/FormatOutPutPrompt.md", "formatOutPut",0),
    ROLE_REVERSAL_CHECK("roleReversalCheck", "./md/RoleReversalCheckPrompt.md", "roleReversalCheckOutput",0),
    MESSAGE_GENERATE("messageGenerateNode", "./md/MessageGeneratePrompt.md", "generateMessage",15),
    INTERACTION_CHECK("interactionCheckOutput", "./md/InteractionCheckPrompt.md", "interactionCheckOutput",0);

    private final String nodeName;
    private final String promptTemplatePath;
    private final String outputKey;
    private final Integer historyMessagesCount;
}
