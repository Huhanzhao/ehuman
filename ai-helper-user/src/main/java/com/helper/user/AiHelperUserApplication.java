package com.helper.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.helper")
@MapperScan("com.helper.user.mapper")
public class AiHelperUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiHelperUserApplication.class, args);
    }

}
