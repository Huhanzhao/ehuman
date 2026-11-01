package com.ehuman.core.bean;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 类的描述：系统配置  环境变量传递
 * 作者：huhanzhao
 * 创建时间：2026/10/31
 */
@Component
@Slf4j
@Getter
@RequiredArgsConstructor
public class SystemConfigBean {

    private final ChatClientFactory chatClientFactory;

    @Value("${bot.prompt-path}")
    private String promptPathPrefix;
}
