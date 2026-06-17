package com.anyfriend.dao.entity;

import com.anyfriend.dao.config.SqliteDateTypeHandler;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 
 * @TableName tb_user_refer_audio
 */
@TableName(value ="tb_user_refer_audio")
@Data
public class TbUserReferAudioDo {
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
     * 声音模型url
     */
    private String referAudioUrl;

    /**
     * 声音模型名字
     */
    private String referAudioName;

    private String type; // local online

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT,typeHandler = SqliteDateTypeHandler.class)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE,typeHandler = SqliteDateTypeHandler.class)
    private Date updateTime;

    /**
     * 删除标识
     */
    @TableLogic
    private Boolean deleteFlag;

    private Boolean enable;
}