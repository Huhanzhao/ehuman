package com.anyfriend.dao.service;

import java.util.Date;
import java.util.List;

/**
 * @Author: huhanzhao
 * @CreateTime: 2026-06-30
 * @Description: 向量操作service
 */
public interface DBVectorOperationService {

    boolean saveVector(Long userId, String vector, String context, Date createTime);

    List<String> searchVector(Long userId, String vector, Integer topK, Double minScore);
}
