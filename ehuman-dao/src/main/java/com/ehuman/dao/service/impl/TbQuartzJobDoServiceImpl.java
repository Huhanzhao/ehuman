package com.ehuman.dao.service.impl;

import com.ehuman.dao.mapper.TbQuartzJobDoMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ehuman.dao.entity.TbQuartzJobDo;
import com.ehuman.dao.service.TbQuartzJobDoService;
import org.springframework.stereotype.Service;

@Service
public class TbQuartzJobDoServiceImpl extends ServiceImpl<TbQuartzJobDoMapper, TbQuartzJobDo>
    implements TbQuartzJobDoService {
    @Override
    public TbQuartzJobDo getByStartName(String jobName) {
        return lambdaQuery().like(TbQuartzJobDo::getJobName, jobName).last("limit 1").one();
    }
}
