package com.helper.services.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName tb_user_voice_model
 */
@TableName(value ="tb_user_voice_model")
@Data
public class TbUserVoiceModelDo {
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 声音模型id
     */
    private String voiceModelUrl;

    /**
     * 声音模型名字
     */
    private String voiceModelName;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 删除标识
     */
    @TableLogic
    private Boolean deleteFlag;

    private Boolean enable;
}