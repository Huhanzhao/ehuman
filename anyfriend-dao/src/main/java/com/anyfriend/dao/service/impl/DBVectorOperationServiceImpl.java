package com.anyfriend.dao.service.impl;

import com.anyfriend.dao.service.DBVectorOperationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * 类的描述：db向量操作实现类
 * 作者：huhanzhao
 * 创建时间：2026/7/5
 */
@Slf4j
@Service
public class DBVectorOperationServiceImpl implements DBVectorOperationService {

    @Autowired
    private DataSource dataSource;

    @Override
    public boolean saveVector(String userId, String vector, String context) {
        try (Connection connection = dataSource.getConnection()){
            Statement stmt = connection.createStatement();
            // 加载向量拓展
            loadVectorExtension(stmt);

            // 插入数据
            stmt.execute("INSERT INTO vector (id,embedding, context)\n" +
                    "VALUES (1222,vector_as_f32('[0.3, 0.2, 0.8, 0.4]'), 'aaaaa');");
            log.info("插入数据成功");

            // 初始化向量
            vectorInit(stmt);

            // 重新索引
            vectorQuantize(stmt);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    private void vectorQuantize(Statement stmt) throws SQLException {
        stmt.execute("SELECT vector_quantize('vector', 'embedding'); ");
        log.info("量化成功");
    }

    private void vectorInit(Statement stmt) throws SQLException {
        stmt.execute("SELECT vector_init('vector', 'embedding', 'dimension=4,type=FLOAT32,distance=cosine');");
        log.info("初始化成功");
    }

    @Override
    public List<String> searchVector(String userId, String vector, Integer topK, Integer minScore) {
        try (Connection connection = dataSource.getConnection()){
            Statement stmt = connection.createStatement();
            ResultSet resultSet = stmt.executeQuery("SELECT \n" +
                    "    e.id,\n" +
                    "    e.context,\n" +
                    "    v.distance,\n" +
                    "    -- 将距离转换为 0-1 之间的相似度分数\n" +
                    "    1.0 - (v.distance / 2) AS similarity_score\n" +
                    "FROM 'vector' e\n" +
                    "JOIN vector_quantize_scan('vector', 'embedding', vector_as_f32('[0.1, 0.2, 0.3, 0.4]'), 20) AS v\n" +
                    "ON e.rowid = v.rowid\n" +
                    "-- 按分数从高到低排序（即按距离从小到大）\n" +
                    "ORDER BY similarity_score DESC;");
            int rowNum = 0;
            while (resultSet.next()) {
                rowNum++;
                String id = resultSet.getString("id");
                String context = resultSet.getString("context");
                float distance = resultSet.getFloat("distance");
                float similarity = resultSet.getFloat("similarity_score");

                System.out.println("=== 第 " + rowNum + " 条 ===");
                System.out.println("ID: " + id);
                System.out.println("Context: " + context);
                System.out.println("Distance: " + distance);
                System.out.println("Similarity Score: " + similarity);
                System.out.println();
            }
            log.info("查询成功");
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadVectorExtension(Statement stmt) throws SQLException {
        // todo 打包镜像换成对应linux插件
        stmt.execute("SELECT load_extension('./sqlite/vector.dll')");
        log.info("加载向量拓展成功");
    }
}
