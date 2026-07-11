package com.anyfriend.dao.service.impl;

import com.anyfriend.dao.entity.TbMessageSummaryVectorDo;
import com.anyfriend.dao.mapper.TbMessageSummaryVectorDoMapper;
import com.anyfriend.dao.service.TbMessageSummaryVectorDoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
    public TbMessageSummaryVectorDo getLastOneByUserId(Long userId) {
        return lambdaQuery()
                .select(TbMessageSummaryVectorDo::getId,
                        TbMessageSummaryVectorDo::getUserId,
                        TbMessageSummaryVectorDo::getContext,
                        TbMessageSummaryVectorDo::getCreateTime)
                .eq(TbMessageSummaryVectorDo::getUserId, userId)
                .orderByDesc(TbMessageSummaryVectorDo::getCreateTime).last("limit 1").one();
    }
}




