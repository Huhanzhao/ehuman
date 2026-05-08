package com.helper.common.entity;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private Long userId;
    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Long userId) {
        super(message);
        this.userId = userId;
    }
}