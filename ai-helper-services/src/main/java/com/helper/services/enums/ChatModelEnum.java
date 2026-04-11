package com.helper.services.enums;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChatModelEnum {

    DEEP_SEEK_V3_2("deepseek-chat","deepseek"),
    GPT_5("gpt-5","gpt"),
    CLAUDE_SONNET_4_5("claude-sonnet-4-5","claude"),
    GROK_4_1_NO_REASONING("grok-4-1-fast-non-reasoning","grok"),
    GPT_4O_MINI("gpt-4o-mini","gpt");

    private final String modelName;
    private final String category;
}
