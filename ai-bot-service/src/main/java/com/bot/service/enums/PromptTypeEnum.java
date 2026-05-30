package com.bot.service.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PromptTypeEnum {
    BOT_ROLE(0),
    SYSTEM_PROMPT(2),
    ASSISTANT_PROMPT(3);

    @EnumValue
    private final Integer type;
}
