package com.bot.service.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;

import com.bot.service.common.config.SqliteDateTypeHandler;
import com.bot.service.enums.NodeEnum;
import lombok.Data;
import lombok.experimental.Accessors;

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
    private NodeEnum nodeEnum;

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