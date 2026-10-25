package com.ehuman.dao.enums;

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

    public static ChatModelCategoryEnum getByCategory(String category){
        for (ChatModelCategoryEnum value : ChatModelCategoryEnum.values()) {
            if (value.getCategory().equals(category)) {
                return value;
            }
        }
        return null;
    }
}
