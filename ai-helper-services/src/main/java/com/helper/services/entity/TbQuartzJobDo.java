package com.helper.services.entity;

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

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
