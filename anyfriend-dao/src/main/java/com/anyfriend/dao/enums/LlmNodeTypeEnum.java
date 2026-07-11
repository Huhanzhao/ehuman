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
    // ----------图节点
    MEMORY_CHECK("memoryCheck", "./md/MemoryCheckPrompt.md", "memoryCheckOutput"),
    FORMAT_OUTPUT("formatOutPut", "./md/FormatOutPutPrompt.md", "formatOutPut"),
    ROLE_REVERSAL_CHECK("roleReversalCheck", "./md/RoleReversalCheckPrompt.md", "roleReversalCheckOutput"),
    MESSAGE_GENERATE("messageGenerateNode", "./md/MessageGeneratePromptV2.md", "generateMessage",15),
    INITIATIVE_MESSAGE_GENERATE("initiativeMessageGenerateNode", "./md/InitiativeMessageGeneratePrompt.md", "generateMessage"),
    INTERACTION_CHECK("interactionCheckOutput", "./md/InteractionCheckPrompt.md", "interactionCheckOutput"),
    COMBINED_CHECK("combinedCheckNode", "./md/CombinedCheckPrompt.md", "combinedCheckOutput"),

    // -----------单次调用节点
    SUMMARY_MESSAGES("SummaryMessage", "./md/SummaryMessagePrompt.md"),
    UPDATE_NEXT_SEND_MESSAGE_INFO("updateNextSendMessageInfo", "./md/NextSendMessage.md");


    private final String nodeName;
    private final String promptTemplatePath;
    private final String outputKey;
    private final Integer historyMessagesCount;

    LlmNodeTypeEnum(String nodeName, String promptTemplatePath){
        this(nodeName,promptTemplatePath,"outPutKey",0);
    }

    LlmNodeTypeEnum(String nodeName, String promptTemplatePath, String outputKey){
        this(nodeName,promptTemplatePath,outputKey,0);
    }
}
