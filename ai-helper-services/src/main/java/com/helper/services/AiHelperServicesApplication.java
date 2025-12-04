package com.helper.services;

import com.helper.commonservicescommon.utils.ChromaUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Date;
import java.util.Map;

@SpringBootApplication(scanBasePackages = "com.helper")
@MapperScan(basePackages = "com.helper.services.mapper")
@Slf4j
public class AiHelperServicesApplication {

    @Autowired
    private ChromaUtils chromaUtils;

    public static void main(String[] args) {
        SpringApplication.run(AiHelperServicesApplication.class, args);
    }

    @PostConstruct
    public void initChroma() {
        // 初始化chroma数据库
        try {
            chromaUtils.createTenant("ai-helper");
        }catch(Exception e){
            log.info("初始化chroma数据库：ai-helper租户已存在");
        }

        try {
            chromaUtils.createDatabase("ai-helper", "ai-service");
        }catch(Exception e){
            log.info("初始化chroma数据库：ai-service数据库已存在");
        }

        try {
            chromaUtils.createCollection("ai-helper", "ai-service",
                    "memory", Map.of("createBy","System",
                            "createTime", DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"),
                            "desc","AI 历史记忆"));
        }catch(Exception e){
            log.info("初始化chroma数据库：memory集合已存在");
        }

        try {
            chromaUtils.createCollection("ai-helper", "ai-service",
                    "knowledge", Map.of("createBy","System",
                            "createTime", DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"),
                            "desc","知识库"));
        }catch(Exception e){
            log.info("初始化chroma数据库：knowledge集合已存在");
        }
    }

}
