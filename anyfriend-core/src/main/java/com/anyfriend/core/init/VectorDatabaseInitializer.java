package com.anyfriend.core.init;

import com.anyfriend.dao.mapper.DbEmbeddingSearchService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class VectorDatabaseInitializer {
    
    @Autowired
    private DataSource dataSource;

    @Autowired
    private DbEmbeddingSearchService dbEmbeddingSearchService;
    
    @PostConstruct
    @Transactional
    public void init() {
        try (Connection conn = dataSource.getConnection()) {
//            // 1. 加载扩展
//            try (Statement stmt = conn.createStatement()) {
//                stmt.execute("SELECT load_extension('./sqlite/vector.dll')");
//                log.info("Vector extension loaded successfully");
//            }
//
//            // 2. 初始化向量表
//            try (Statement stmt = conn.createStatement()) {
//                stmt.execute("SELECT vector_init('vector', 'embedding', 'dimension=4,type=FLOAT32,distance=cosine')");
//                log.info("Vector initialized successfully");
//            }
//            try (Statement stmt = conn.createStatement()) {
//                stmt.execute("SELECT vector_quantize('vector', 'embedding');");
//                log.info("Vector initialized successfully");
//            }
//            try (Statement stmt = conn.createStatement()) {
//                boolean execute = stmt.execute("SELECT distinct \n" +
//                        "    e.id,\n" +
//                        "    e.context,\n" +
//                        "    v.distance,\n" +
//                        "    1.0 - (v.distance / 2) AS similarity_score " +
//                        " FROM vector as e\n " +
//                        " JOIN vector_quantize_scan('vector', 'embedding', vector_as_f32('[0.1, 0.2, 0.3, 0.4]'), 20) AS v\n" +
//                        " ON e.id = v.rowid");
//                log.info("Vector initialized successfully");
//                ResultSet rs = stmt.getResultSet();
//                while (rs.next()) {
//                    int id = rs.getInt("id");
//                    String context = rs.getString("context");
//                    double distance = rs.getDouble("distance");
//                    double similarityScore = rs.getDouble("similarity_score");
//
//                    log.info("id: {}, context: {}, distance: {}, similarity_score: {}",
//                            id, context, distance, similarityScore);
//                }
//                rs.close();
//            }

//            dbEmbeddingSearchService.save("aaa","bbbb");
//            List<Map<String,Object>> sdfsf = dbEmbeddingSearchService.search("sdfsf", 2, 2);
//            System.out.println(sdfsf.toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize vector database", e);
        }
    }
}