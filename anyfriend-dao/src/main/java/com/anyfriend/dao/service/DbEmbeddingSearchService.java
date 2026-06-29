package com.anyfriend.dao.service;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: huhanzhao
 * @CreateTime: 2026-06-29
 * @Description: 向量检索插入服务
 */
@Service
public interface DbEmbeddingSearchService {

    @Select("")
    List<String> search(String vector, Integer topK, Integer score);

    @Insert("")
    boolean save(String vector, String context);
}
