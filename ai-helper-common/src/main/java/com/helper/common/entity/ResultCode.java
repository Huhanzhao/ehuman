package com.helper.common.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "成功"),
    FAIL(500, "失败"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    TOKEN_EXPIRED(6001, "Token 已过期"),
    TOKEN_INVALID(6002, "Token 无效"),
    TOKEN_KICKED(6003, "账号已在其他设备登录");

    private final int code;
    private final String message;
}