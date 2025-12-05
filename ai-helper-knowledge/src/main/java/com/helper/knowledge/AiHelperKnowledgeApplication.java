package com.helper.knowledge;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.helper"})
@MapperScan("com.helper.knowledge.mapper")
public class AiHelperKnowledgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiHelperKnowledgeApplication.class, args);
    }

}
