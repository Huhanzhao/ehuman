package com.ehuman.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 
 * @TableName tb_bot_prompt
 */
@TableName(value ="tb_bot_prompt")
@Data
public class TbBotPromptDo {
    /**
     * 
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Integer id;

    private String name;
    /**
     * 
     */
    private String content;

    /**
     * 
     */
    @TableField("enable")
    private Boolean enable;

    /**
     * 
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}