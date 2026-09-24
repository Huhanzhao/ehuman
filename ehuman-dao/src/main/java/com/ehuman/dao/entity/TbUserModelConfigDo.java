package com.ehuman.dao.entity;

import com.ehuman.dao.config.SqliteDateTypeHandler;
import com.ehuman.dao.enums.LlmTypeEnum;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 
 * @TableName tb_user_model_config
 */
@TableName(value ="tb_user_model_config")
@Data
@Accessors(chain = true)
public class TbUserModelConfigDo {
    /**
     * 
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 
     */
    private String model;

    private String category;

    /**
     * 
     */
    @TableField("node_num")
    private LlmTypeEnum llmTypeEnum;

    /**
     * 
     */
    private Long userId;

    /**
     * 
     */
    private String baseUrl;

    /**
     * 
     */
    private String apiKey;

    /**
     * 
     */
    @TableField(fill = FieldFill.INSERT_UPDATE,typeHandler = SqliteDateTypeHandler.class)
    private Date updateTime;

    /**
     * 
     */
    @TableField(fill = FieldFill.INSERT,typeHandler = SqliteDateTypeHandler.class)
    private Date createTime;

    /**
     * 
     */
    @TableLogic
    private Boolean deleteFlag;

    private Boolean needProxy;

}