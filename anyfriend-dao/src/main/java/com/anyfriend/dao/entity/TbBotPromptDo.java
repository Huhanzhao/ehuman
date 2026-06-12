package com.anyfriend.dao.entity;

import com.anyfriend.dao.enums.PromptTypeEnum;
import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;

import lombok.Data;

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

    /**
     * 
     */
    private String content;

    /**
     * 
     */
    @TableField("type")
    private PromptTypeEnum promptTypeEnum;

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
        TbBotPromptDo other = (TbBotPromptDo) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getContent() == null ? other.getContent() == null : this.getContent().equals(other.getContent()))
            && (this.getPromptTypeEnum() == null ? other.getPromptTypeEnum() == null : this.getPromptTypeEnum().equals(other.getPromptTypeEnum()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getContent() == null) ? 0 : getContent().hashCode());
        result = prime * result + ((getPromptTypeEnum() == null) ? 0 : getPromptTypeEnum().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", content=").append(content);
        sb.append(", type=").append(this.getPromptTypeEnum().getType());
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append("]");
        return sb.toString();
    }
}