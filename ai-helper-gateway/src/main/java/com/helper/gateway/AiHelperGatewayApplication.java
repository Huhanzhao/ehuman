package com.helper.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.CrossOrigin;

@SpringBootApplication(scanBasePackages = "com.helper")
@CrossOrigin
@EnableDiscoveryClient
public class AiHelperGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiHelperGatewayApplication.class, args);
    }

}
