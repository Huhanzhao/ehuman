package com.bot.service.common.config;


import com.bot.service.common.entity.BusinessException;
import com.bot.service.common.entity.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 业务异常  todo 错误发送给qq
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleTokenExpired(BusinessException e) {
        log.error(e.getMessage());
        return Result.fail(e.getMessage());
    }

    // 其他运行时异常
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error(e.getMessage());
        return Result.fail("系统错误: " + e.getMessage());
    }
}