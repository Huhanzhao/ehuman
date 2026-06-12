package com.anyfriend.dao.entity;

import com.anyfriend.dao.config.SqliteDateTypeHandler;
import com.anyfriend.dao.enums.MoodEnum;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 
 * @TableName tb_user_bot_status
 */
@TableName(value ="tb_user_bot_status")
@Accessors(chain = true)
@Data
public class TbUserBotStatusDo {
    /**
     *
     * -- GETTER --
     *

     */
    @TableId(value = "ID", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户id
     * -- GETTER --
     *  用户id

     */
    private Long userId;

    /**
     * 用户好感度0-100
     * -- GETTER --
     *  用户好感度0-100

     */
    private Integer grace;

    /**
     *  bot 亲密度 familiarity
     */
    private Integer familiarity;


    /**
     * bot心情
     *  bot心情
     */
    @TableField("mood")
    private MoodEnum moodEnum;

    // 心情原因
    private String moodReason;


    /**
     * 创建人
     * -- GETTER --
     *  创建人

     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间
     * -- GETTER --
     *  创建时间

     */
    @TableField(fill = FieldFill.INSERT,typeHandler = SqliteDateTypeHandler.class)
    private Date createTime;

    /**
     * 更新人
     * -- GETTER --
     *  更新人

     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 更新时间
     * -- GETTER --
     *  更新时间

     */
    @TableField(fill = FieldFill.INSERT_UPDATE,typeHandler = SqliteDateTypeHandler.class)
    private Date updateTime;

    /**
     * 删除标识
     * -- GETTER --
     *  删除标识

     */
    @TableLogic
    private Boolean deleteFlag;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        TbUserBotStatusDo other = (TbUserBotStatusDo) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getGrace() == null ? other.getGrace() == null : this.getGrace().equals(other.getGrace()))
            && (this.getCreateBy() == null ? other.getCreateBy() == null : this.getCreateBy().equals(other.getCreateBy()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateBy() == null ? other.getUpdateBy() == null : this.getUpdateBy().equals(other.getUpdateBy()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()))
            && (this.getDeleteFlag() == null ? other.getDeleteFlag() == null : this.getDeleteFlag().equals(other.getDeleteFlag()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getGrace() == null) ? 0 : getGrace().hashCode());
        result = prime * result + ((getCreateBy() == null) ? 0 : getCreateBy().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateBy() == null) ? 0 : getUpdateBy().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        result = prime * result + ((getDeleteFlag() == null) ? 0 : getDeleteFlag().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", userId=").append(userId);
        sb.append(", grace=").append(grace);
        sb.append(", createBy=").append(createBy);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateBy=").append(updateBy);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", deleteFlag=").append(deleteFlag);
        sb.append("]");
        return sb.toString();
    }
}