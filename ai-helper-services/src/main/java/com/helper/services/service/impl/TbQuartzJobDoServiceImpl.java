package com.helper.services.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.helper.services.entity.TbQuartzJobDo;
import com.helper.services.mapper.TbQuartzJobDoMapper;
import com.helper.services.service.TbQuartzJobDoService;
import org.springframework.stereotype.Service;

@Service
public class TbQuartzJobDoServiceImpl extends ServiceImpl<TbQuartzJobDoMapper, TbQuartzJobDo>
    implements TbQuartzJobDoService {
}
