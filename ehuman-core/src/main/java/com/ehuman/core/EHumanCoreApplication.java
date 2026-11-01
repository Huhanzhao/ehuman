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
        // 强制关闭无头模式，允许弹窗
        System.setProperty("java.awt.headless", "false");

        SpringApplication.run(EHumanCoreApplication.class, args);
    }

}
