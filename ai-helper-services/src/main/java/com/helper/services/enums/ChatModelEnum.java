package com.helper.services.enums;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChatModelEnum {

    DEEP_SEEK_V3_2("deepseek-chat"),
    GPT_4O_MINI("gpt-4o-mini");

    private final String modelName;
}
