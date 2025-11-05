package com.ai.langchai4j.aihelper;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.ai")
@MapperScan(basePackages = "com.ai.langchai4j.aihelper.demand.mapper")
public class AiHelperApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiHelperApplication.class, args);
    }

}
