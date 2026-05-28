package com.bot.service.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.bot.service.common.config.SqliteDateTypeHandler;
import lombok.Data;

import java.util.Date;

/**
 * 
 * @TableName tb_user_info
 */
@TableName(value ="tb_user_info")
@Data
public class TbUserInfoDo {
    /**
     * 主键id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户信息
     */
    private String userInfo;


    @TableField(fill = FieldFill.INSERT,typeHandler = SqliteDateTypeHandler.class)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE,typeHandler = SqliteDateTypeHandler.class)
    private Date updateTime;
}