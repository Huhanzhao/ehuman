package com.anyfriend.dao.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anyfriend.dao.entity.TbMessageSummaryVectorDo;
import com.anyfriend.dao.service.TbMessageSummaryVectorDoService;
import com.anyfriend.dao.mapper.TbMessageSummaryVectorDoMapper;
import org.springframework.stereotype.Service;

/**
* @author huhz4
* @description 针对表【tb_message_summary_vector】的数据库操作Service实现
* @createDate 2026-07-07 17:34:27
*/
@Service
public class TbMessageSummaryVectorDoServiceImpl extends ServiceImpl<TbMessageSummaryVectorDoMapper, TbMessageSummaryVectorDo>
    implements TbMessageSummaryVectorDoService{

    @Override
    public TbMessageSummaryVectorDo getLastOneByUserId() {
        return lambdaQuery().orderByDesc(TbMessageSummaryVectorDo::getCreateTime).last("limit 1").one();
    }
}




