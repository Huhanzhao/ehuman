package com.bot.service.common.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Result<T> {

    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success() {
        return Result.<T>builder()
                .code(ResultCode.SUCCESS.getCode())
                .message(ResultCode.SUCCESS.getMessage())
                .build();
    }

    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(ResultCode.SUCCESS.getCode())
                .message(ResultCode.SUCCESS.getMessage())
                .data(data)
                .build();
    }

    public static <T> Result<T> fail(String msg) {
        return Result.<T>builder()
                .code(ResultCode.FAIL.getCode())
                .message(msg)
                .build();
    }

    public static <T> Result<T> fail(ResultCode code) {
        return Result.<T>builder()
                .code(code.getCode())
                .message(code.getMessage())
                .build();
    }
}
