package com.anyfriend.dao.entity;

import com.anyfriend.dao.config.SqliteDateTypeHandler;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@TableName("tb_quartz_job")
@Data
public class TbQuartzJobDo {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String jobName;

    private String jobDetail;

    private String jobGroup;

    private String cron;

    private Boolean status;

    private String className;

    @TableField(fill = FieldFill.INSERT,typeHandler = SqliteDateTypeHandler.class)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE,typeHandler = SqliteDateTypeHandler.class)
    private Date updateTime;

    @TableLogic
    private Boolean deleted;
}
