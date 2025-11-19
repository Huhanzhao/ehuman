package com.helper.gateway.config;

import com.helper.common.entity.BusinessException;
import com.helper.common.entity.Result;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GatewayGlobalExceptionHandler {

    // 业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleTokenExpired(BusinessException e) {
        return Result.fail(e.getMessage());
    }

    // 其他运行时异常
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        return Result.fail("系统错误: " + e.getMessage());
    }
}