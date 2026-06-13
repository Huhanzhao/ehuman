package com.anyfriend.dao.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChatModelCategoryEnum {

    DEEP_SEEK("deepseek"),
    GPT("gpt"),
    CLAUDE("claude"),
    GROK("grok");

    private final String category;
}
