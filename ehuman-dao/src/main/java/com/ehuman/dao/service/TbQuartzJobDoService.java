package com.ehuman.dao.service;

import com.ehuman.dao.entity.TbQuartzJobDo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface TbQuartzJobDoService extends IService<TbQuartzJobDo> {
    TbQuartzJobDo getByStartName(String jobName);
}
