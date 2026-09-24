package com.ehuman.dao.service.impl;

import com.ehuman.dao.entity.TbMessageVectorSearchDo;
import com.ehuman.dao.service.DBVectorOperationService;
import com.ehuman.dao.service.TbMessageSummaryVectorDoService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

    @Value("${sqlite.vector}")
    private String vectorPlugin;

    @Autowired
    private TbMessageSummaryVectorDoService tbMessageSummaryVectorDoService;

    @Override
    public boolean saveVector(Long userId, String vector, String context, Date createTime) {
        try (Connection connection = dataSource.getConnection()){
            Statement stmt = connection.createStatement();
            // 加载向量拓展
            loadVectorExtension(stmt);

            // 插入数据
            insertIntoMessagesVector(connection,userId, vector, context, createTime);

            // 初始化向量
            vectorInit(stmt);

            // 重新索引
            vectorQuantize(stmt);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
    private void insertIntoMessagesVector(Connection connection,Long userId, String embedding, String context, Date createTime) {
        String sql = "INSERT INTO tb_message_summary_vector (user_id,embedding, context,create_time)\n" +
                "VALUES (?,vector_as_f32(?), ?,?);";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, embedding);
            stmt.setString(3, context);
            // 格式化为字符串存储
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timeStr = sdf.format(createTime);
            stmt.setString(4, timeStr);  // 存为 TEXT
            stmt.executeUpdate();
            log.info("插入数据成功");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void vectorQuantize(Statement stmt) throws SQLException {
        stmt.execute("SELECT vector_quantize('tb_message_summary_vector', 'embedding'); ");
        log.info("量化成功");
    }

    private void vectorInit(Statement stmt) throws SQLException {
        // 修改维度
        stmt.execute("SELECT vector_init('tb_message_summary_vector', 'embedding', 'dimension=1024,type=FLOAT32,distance=cosine');");
        log.info("初始化成功");
    }

    @Override
    public List<TbMessageVectorSearchDo> searchVector(Long userId, String vector, Integer topK, Double minScore) {
        try (Connection connection = dataSource.getConnection()){
            Statement stmt = connection.createStatement();

            // 加载向量拓展
            loadVectorExtension(stmt);

            // 初始化向量
            vectorInit(stmt);

            // 重新索引
            vectorQuantize(stmt);

            // 查询
            String sql = "SELECT \n" +
                    "    e.id,\n" +
                    "    e.user_id,\n" +
                    "    e.context,\n" +
                    "    v.distance,\n" +
                    "    -- 将距离转换为 0-1 之间的相似度分数\n" +
                    "    1.0 - (v.distance / 2) AS similarity_score,\n" +
                    "    e.create_time\n" +
                    "FROM tb_message_summary_vector e\n" +
                    "JOIN vector_quantize_scan('tb_message_summary_vector', 'embedding', vector_as_f32(?), 10000) AS v\n" +
                    "ON e.id = v.rowid\n" +
                    "where e.user_id = ?\n and similarity_score > ?" +
                    "-- 按分数从高到低排序（即按距离从小到大）\n" +
                    "ORDER BY similarity_score DESC limit ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, vector);
            preparedStatement.setLong(2, userId);
            preparedStatement.setDouble(3, minScore);
            preparedStatement.setInt(4, topK);

            ResultSet resultSet = preparedStatement.executeQuery();
            List<TbMessageVectorSearchDo> resultList = new ArrayList<>();

            while (resultSet.next()) {
                TbMessageVectorSearchDo entity = new TbMessageVectorSearchDo();
                entity.setId(resultSet.getString("id"));
                entity.setContext(resultSet.getString("context"));
                entity.setDistance(resultSet.getFloat("distance"));
                entity.setSimilarityScore(resultSet.getFloat("similarity_score"));
                entity.setCreateTime(resultSet.getString("create_time"));
                resultList.add(entity);
            }
            log.info("查询成功");
            return resultList;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadVectorExtension(Statement stmt) throws SQLException {
        // 打包镜像换成对应linux插件
        stmt.execute("SELECT load_extension('./sqlite/" + vectorPlugin + "')");
        log.info("加载向量拓展成功");
    }

    @PostConstruct
    private void initVectorTable(){
        try (Connection connection = dataSource.getConnection()){
            Statement stmt = connection.createStatement();
            // 加载向量拓展
            loadVectorExtension(stmt);

            //判断是否存在向量表
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " +
                    "sqlite_master WHERE type='table' " +
                    "AND name='tb_message_summary_vector'");

            // 判断表是否存在
            if (rs.next()) {
                int count = rs.getInt(1);  // 或 rs.getInt("COUNT(*)")
                if (count > 0) {
                    log.info("向量表存在跳过初始化");
                } else {
                    log.info("向量表不存在开始初始化");
                        // 创建向量表
                    stmt.execute("""
                        CREATE TABLE "tb_message_summary_vector" (
                         "id" INTEGER PRIMARY KEY AUTOINCREMENT,
                         "user_id" bigint NOT NULL,
                         "embedding" BLOB,
                         "context" TEXT,
                         "create_time" date
                        );
                    """);
                    log.info("初始化成功");
                }
            }
            rs.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
