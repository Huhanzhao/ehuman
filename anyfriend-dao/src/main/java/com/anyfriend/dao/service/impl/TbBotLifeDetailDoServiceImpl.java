package com.anyfriend.dao.service.impl;

import com.anyfriend.dao.mapper.TbBotLifeDetailDoMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anyfriend.dao.entity.TbBotLifeDetailDo;
import com.anyfriend.dao.service.TbBotLifeDetailDoService;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
* @author Lenovo
* @description 针对表【tb_bot_life_detail】的数据库操作Service实现
* @createDate 2026-05-11 21:56:55
*/
@Service
public class TbBotLifeDetailDoServiceImpl extends ServiceImpl<TbBotLifeDetailDoMapper, TbBotLifeDetailDo>
    implements TbBotLifeDetailDoService{

    @Override
    public String getCurrentActivity(Long botId) {
        // 查询bot当前正在做的事
        Date now = new Date();
        TbBotLifeDetailDo currentLife = this.getOne(
                Wrappers.<TbBotLifeDetailDo>lambdaQuery()
                        .le(TbBotLifeDetailDo::getStartTime, now)
                        .eq(TbBotLifeDetailDo::getBotId,botId)
                        .ge(TbBotLifeDetailDo::getEndTime, now)
                        .last("limit 1")
        );
        return currentLife != null ? currentLife.getDetail() : "";
    }
}




