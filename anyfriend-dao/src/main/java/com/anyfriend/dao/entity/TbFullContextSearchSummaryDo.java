package com.anyfriend.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 全文检索摘要实体类（FTS5虚拟表）
 */
@Data
@TableName("tb_full_context_search_summary")
public class TbFullContextSearchSummaryDo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 内容（全文检索的主要字段）
     */
    @TableField("content")
    private String content;

    /**
     * 元数据（JSON格式存储扩展信息）
     */
    @TableField("metadata")
    private String metadata;

}