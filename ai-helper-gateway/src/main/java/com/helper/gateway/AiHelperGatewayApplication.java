package com.helper.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.helper")
public class AiHelperGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiHelperGatewayApplication.class, args);
    }

}
