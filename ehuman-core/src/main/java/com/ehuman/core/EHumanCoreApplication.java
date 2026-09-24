package com.ehuman.core;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.ehuman")
@MapperScan(basePackages = "com.ehuman.dao.mapper")
@Slf4j
public class EHumanCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(EHumanCoreApplication.class, args);
    }

}
