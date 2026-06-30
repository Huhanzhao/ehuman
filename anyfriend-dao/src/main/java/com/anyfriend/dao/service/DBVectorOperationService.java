package com.anyfriend.dao.service;

import java.util.List;

/**
 * @Author: huhanzhao
 * @CreateTime: 2026-06-30
 * @Description: 向量操作service
 */
public interface DBVectorOperationService {

    boolean saveVector(String userId, String vector, String context);

    List<String> searchVector(String userId, String vector, Integer topK, Integer minScore);
}
