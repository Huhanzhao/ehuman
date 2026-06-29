package com.anyfriend.dao.service;

import com.anyfriend.dao.entity.TbQuartzJobDo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface TbQuartzJobDoService extends IService<TbQuartzJobDo> {
    TbQuartzJobDo getByName(String jobName);
}
