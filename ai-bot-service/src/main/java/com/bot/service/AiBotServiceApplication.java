package com.bot.service;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.bot")
@MapperScan(basePackages = "com.bot.service.mapper")
@Slf4j
public class AiBotServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiBotServiceApplication.class, args);
    }

}
