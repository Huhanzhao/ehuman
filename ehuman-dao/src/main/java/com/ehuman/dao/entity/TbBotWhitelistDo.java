package com.ehuman.dao.entity;

import com.ehuman.dao.config.SqliteDateTypeHandler;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 白名单用户
 * @TableName tb_bot_whitelist
 */
@TableName(value = "tb_bot_whitelist")
@Data
@Accessors(chain = true)
public class TbBotWhitelistDo {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String nickname;

    @TableField(fill = FieldFill.INSERT, typeHandler = SqliteDateTypeHandler.class)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE, typeHandler = SqliteDateTypeHandler.class)
    private Date updateTime;

    @TableLogic
    private Boolean deleteFlag;
}
