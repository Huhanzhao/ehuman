package com.anyfriend.dao.entity;

import com.anyfriend.dao.config.SqliteDateTypeHandler;
import com.anyfriend.dao.enums.FeatureTypeEnum;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 
 * @TableName tb_user_feature_button
 */
@TableName(value ="tb_user_feature_button")
@Data
public class TbUserFeatureButtonDo {
    /**
     * 
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 
     */
    @TableField("feature_type")
    private FeatureTypeEnum featureTypeEnum;

    /**
     * 
     */
    private Long userId;

    /**
     * 
     */
    private Boolean enable;

    /**
     * 
     */
    @TableField(fill = FieldFill.INSERT,typeHandler = SqliteDateTypeHandler.class)
    private Date createTime;

    /**
     * 
     */
    @TableField(fill = FieldFill.INSERT_UPDATE,typeHandler = SqliteDateTypeHandler.class)
    private Date updateTime;
}