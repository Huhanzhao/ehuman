package com.anyfriend.dao.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChatModelCategoryEnum {

    IMAGE("image"),
    GPT("gpt"),
    CLAUDE("claude"),
    TTS("tts"),
    EMBEDDING("embedding"),
    GROK("grok");

    private final String category;
}
