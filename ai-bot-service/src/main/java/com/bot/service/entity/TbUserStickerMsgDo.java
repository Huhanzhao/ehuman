//package com.bot.service.entity;
//
//import com.baomidou.mybatisplus.annotation.*;
//import lombok.Data;
//import lombok.experimental.Accessors;
//
//import java.util.Date;
//
///**
// *
// * @TableName tb_user_sticker_msg
// */
//@TableName(value ="tb_user_sticker_msg")
//@Data
//@Accessors(chain = true)
//public class TbUserStickerMsgDo {
//    /**
//     *
//     */
//    @TableId(type = IdType.ASSIGN_ID)
//    private Long id;
//
//    /**
//     *
//     */
//    private Long userId;
//
//    /**
//     *
//     */
//    private String stickerMsgId;
//
//    /**
//     *
//     */
//    private String emo;
//
//    /**
//     *
//     */
//    private String desc;
//
//    /**
//     *
//     */
//    @TableField(fill = FieldFill.INSERT)
//    private Date createTime;
//
//    /**
//     *
//     */
//    @TableField(fill = FieldFill.INSERT_UPDATE)
//    private Date updateTime;
//
//    @Override
//    public boolean equals(Object that) {
//        if (this == that) {
//            return true;
//        }
//        if (that == null) {
//            return false;
//        }
//        if (getClass() != that.getClass()) {
//            return false;
//        }
//        TbUserStickerMsgDo other = (TbUserStickerMsgDo) that;
//        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
//            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
//            && (this.getStickerMsgId() == null ? other.getStickerMsgId() == null : this.getStickerMsgId().equals(other.getStickerMsgId()))
//            && (this.getEmo() == null ? other.getEmo() == null : this.getEmo().equals(other.getEmo()))
//            && (this.getDesc() == null ? other.getDesc() == null : this.getDesc().equals(other.getDesc()))
//            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
//            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
//    }
//
//    @Override
//    public int hashCode() {
//        final int prime = 31;
//        int result = 1;
//        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
//        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
//        result = prime * result + ((getStickerMsgId() == null) ? 0 : getStickerMsgId().hashCode());
//        result = prime * result + ((getEmo() == null) ? 0 : getEmo().hashCode());
//        result = prime * result + ((getDesc() == null) ? 0 : getDesc().hashCode());
//        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
//        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
//        return result;
//    }
//
//    @Override
//    public String toString() {
//        StringBuilder sb = new StringBuilder();
//        sb.append(getClass().getSimpleName());
//        sb.append(" [");
//        sb.append("Hash = ").append(hashCode());
//        sb.append(", id=").append(id);
//        sb.append(", userId=").append(userId);
//        sb.append(", stickerMsgId=").append(stickerMsgId);
//        sb.append(", emo=").append(emo);
//        sb.append(", desc=").append(desc);
//        sb.append(", createTime=").append(createTime);
//        sb.append(", updateTime=").append(updateTime);
//        sb.append("]");
//        return sb.toString();
//    }
//}