package com.helper.user.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
@Getter
@AllArgsConstructor
public enum UserRoleEnum implements IEnum<Integer> {

    SUPER_ADMIN(1, "超级管理员"),
    ADMIN(2, "管理员"),
    USER(3, "普通用户");

    private final int code;
    private final String desc;

    public static UserRoleEnum fromCode(int code) {
        for (UserRoleEnum role : values()) {
            if (role.code == code) return role;
        }
        throw new IllegalArgumentException("未知的角色 code: " + code);
    }

    @Override
    public Integer getValue() {
        return code;
    }
}
