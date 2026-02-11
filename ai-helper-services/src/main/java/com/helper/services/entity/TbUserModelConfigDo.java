package com.helper.services.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName tb_user_model_config
 */
@TableName(value ="tb_user_model_config")
@Data
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
    private Integer nodeNum;

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
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 
     */
    @TableLogic
    private Boolean deleteFlag;

}