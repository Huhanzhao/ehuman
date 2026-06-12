package com.anyfriend.core;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.anyfriend")
@MapperScan(basePackages = "com.anyfriend.dao.mapper")
@Slf4j
public class AnyFriendCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnyFriendCoreApplication.class, args);
    }

}
