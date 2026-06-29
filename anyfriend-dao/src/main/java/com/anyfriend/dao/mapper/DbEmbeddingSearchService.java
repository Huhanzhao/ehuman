package com.anyfriend.dao.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @Author: huhanzhao
 * @CreateTime: 2026-06-29
 * @Description: 向量检索插入服务
 */
@Service
@Mapper
public interface DbEmbeddingSearchService {

    @Select("SELECT *" +
            "FROM vector e\n" +
            "JOIN vector_quantize_scan('vector', 'embedding', vector_as_f32('[0.1, 0.2, 0.3, 0.4]'), 20) AS v\n" +
            "ON e.id = v.rowid")
    List<Map<String,Object>> search(String vector, Integer topK, Integer score);

    @Insert("INSERT INTO vector (id,embedding, context)\n" +
            "VALUES (13,vector_as_f32('[0.3, 0.2, 0.8, 0.4]'), 'aaaaa');" +
            "SELECT vector_quantize('vector', 'embedding');")
    boolean save(String vector, String context);
}
