package com.helper.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.CrossOrigin;

@SpringBootApplication(scanBasePackages = "com.helper")
@MapperScan("com.helper.user.mapper")
@CrossOrigin
@EnableDiscoveryClient
public class AiHelperUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiHelperUserApplication.class, args);
    }

}
