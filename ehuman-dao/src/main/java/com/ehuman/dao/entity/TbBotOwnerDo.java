package com.ehuman.dao.entity;

import com.ehuman.dao.config.SqliteDateTypeHandler;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 机器人主人（单行表，id 固定为 1）
 * @TableName tb_bot_owner
 */
@TableName(value = "tb_bot_owner")
@Data
@Accessors(chain = true)
public class TbBotOwnerDo {
    @TableId(type = IdType.INPUT)
    private Long id;

    private Long userId;

    @TableField(fill = FieldFill.INSERT, typeHandler = SqliteDateTypeHandler.class)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE, typeHandler = SqliteDateTypeHandler.class)
    private Date updateTime;
}
