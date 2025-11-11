package com.helper.services;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.helper")
@MapperScan(basePackages = "com.helper.services.mapper")
public class AiHelperServicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiHelperServicesApplication.class, args);
    }

}
