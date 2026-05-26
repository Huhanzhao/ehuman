package com.bot.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bot.service.entity.TbUserBotStatusDo;
import com.bot.service.service.TbUserBotStatusDoService;
import com.bot.service.mapper.TbUserBotStatusDoMapper;
import org.springframework.stereotype.Service;

/**
* @author Lenovo
* @description 针对表【tb_user_bot_status】的数据库操作Service实现
* @createDate 2026-01-25 15:37:28
*/
@Service
public class TbUserBotStatusDoServiceImpl extends ServiceImpl<TbUserBotStatusDoMapper, TbUserBotStatusDo>
    implements TbUserBotStatusDoService {

    @Override
    public TbUserBotStatusDo getByUserId(Long userId) {
        return lambdaQuery()
                .eq(TbUserBotStatusDo::getUserId, userId)
                .one();
    }

    @Override
    public void updateGraceByUserId(Long userId, Integer grace) {
        lambdaUpdate()
                .set(TbUserBotStatusDo::getGrace, grace)
                .eq(TbUserBotStatusDo::getUserId, userId)
                .update();
    }

    @Override
    public void updateFamiliarityByUserId(Long userId, Integer familiarity) {
        lambdaUpdate()
                .set(TbUserBotStatusDo::getFamiliarity, familiarity)
                .eq(TbUserBotStatusDo::getUserId, userId)
                .update();
    }
}




