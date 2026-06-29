package com.anyfriend.core.init;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Slf4j
@Component
public class VectorDatabaseInitializer {
    
    @Autowired
    private DataSource dataSource;
    
    @PostConstruct
    public void init() {
        try (Connection conn = dataSource.getConnection()) {
            // 1. 加载扩展
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT load_extension('./sqlite/vector.dll')");
                log.info("Vector extension loaded successfully");
            }
            
            // 2. 初始化向量表
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT vector_init('vector', 'embedding', 'dimension=4,type=FLOAT32,distance=cosine')");
                log.info("Vector initialized successfully");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize vector database", e);
        }
    }
}