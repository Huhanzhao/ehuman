package com.ehuman.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 
 * @TableName tb_chat_config
 */
@TableName(value ="tb_chat_config")
@Data
public class TbChatConfigDo {
    /**
     * 
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 
     */
    private Double temperature;
    private Double topP;

    /**
     * 
     */
    private Long userId;


}