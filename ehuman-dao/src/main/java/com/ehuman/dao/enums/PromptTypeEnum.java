package com.ehuman.dao.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PromptTypeEnum {
    BOT_ROLE(0),
    TARGET(1),
    ASSISTANT_PROMPT(3);

    @EnumValue
    private final Integer type;
}
