package com.anyfriend.dao.entity;

import lombok.Data;

/**
 * @Author: huhanzhao
 * @CreateTime: 2026-07-07
 * @Description: 向量检索结果实体
 */
@Data
public class TbMessageVectorSearchDo {
    private String id;
    private String context;
    private Float distance;
    private Float similarityScore;
    private String createTime;
}
