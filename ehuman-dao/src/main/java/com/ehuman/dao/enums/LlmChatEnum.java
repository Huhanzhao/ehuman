package com.ehuman.dao.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: huhanzhao
 * @CreateTime: 2026-06-11
 * @Description: LlmNode类型枚举
 */

@Getter
@AllArgsConstructor
public enum LlmChatEnum {
    // ----------图节点
    MEMORY_CHECK("memoryCheck", "MemoryCheckPrompt.md", "memoryCheckOutput"),
    FORMAT_OUTPUT("formatOutPut", "FormatOutPutPrompt.md", "formatOutPut"),
    ROLE_REVERSAL_CHECK("roleReversalCheck", "RoleReversalCheckPrompt.md", "roleReversalCheckOutput"),
    MESSAGE_GENERATE("messageGenerateNode", "MessageGeneratePromptV2.md", "generateMessage",15),
    INITIATIVE_MESSAGE_GENERATE("initiativeMessageGenerateNode", "InitiativeMessageGeneratePrompt.md", "generateMessage"),
    INTERACTION_CHECK("interactionCheckOutput", "InteractionCheckPrompt.md", "interactionCheckOutput"),
    COMBINED_CHECK("combinedCheckNode", "CombinedCheckPrompt.md", "combinedCheckOutput"),

    // -----------单次调用节点
    SUMMARY_MESSAGES("SummaryMessage", "SummaryMessagePrompt.md"),
    USER_WANT_TO("userWantTo", "UserWantToPrompt.md"),
    UPDATE_NEXT_SEND_MESSAGE_INFO("updateNextSendMessageInfo", "NextSendMessage.md"),
    MOOD_ANALYSIS("moodAnalysis", "MoodAnalysis.md"),
    IMAGE_ANALYSIS("imageAnalysis", "ImageAnalysisPrompt.md"),
    LIFE_DETAIL_GENERATE("lifeDetailGenerate", "BotLifeDetailPrompt.md"),
    USER_INFO_AND_GRACE_JOB("userInfoAndGraceJob", "UserInfoAndGraceJobPrompt.md");


    private final String nodeName;
    private final String promptFileName;
    private final String outputKey;
    private final Integer historyMessagesCount;

    LlmChatEnum(String nodeName, String promptFileName){
        this(nodeName,promptFileName,"outPutKey",0);
    }

    LlmChatEnum(String nodeName, String promptFileName, String outputKey){
        this(nodeName,promptFileName,outputKey,0);
    }
}
