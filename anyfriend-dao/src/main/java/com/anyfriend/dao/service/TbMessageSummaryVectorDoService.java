package com.anyfriend.dao.service;

import com.anyfriend.dao.entity.TbMessageSummaryVectorDo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author huhz4
* @description 针对表【tb_message_summary_vector】的数据库操作Service
* @createDate 2026-07-07 17:34:27
*/
public interface TbMessageSummaryVectorDoService extends IService<TbMessageSummaryVectorDo> {

    TbMessageSummaryVectorDo getLastOneByUserId();

}
