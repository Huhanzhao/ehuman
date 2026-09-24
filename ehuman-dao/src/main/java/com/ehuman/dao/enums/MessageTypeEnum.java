package com.ehuman.dao.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageTypeEnum {


    AI_MESSAGE("assistant"),
    USER_MESSAGE("user"),
    SYSTEM("system");

    @EnumValue
    private final String role;
}
