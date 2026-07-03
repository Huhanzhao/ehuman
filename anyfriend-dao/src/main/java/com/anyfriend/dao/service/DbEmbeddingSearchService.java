package com.anyfriend.dao.service;

import java.util.List;
import java.util.Map;

/**
 * @Author: huhanzhao
 * @CreateTime: 2026-06-29
 * @Description: 向量检索插入服务
 */
public interface DbEmbeddingSearchService {

    List<Map<String,Object>> search(String vector, Integer topK, Integer score);

    boolean save(String vector, String context);
}
