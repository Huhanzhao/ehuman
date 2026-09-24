package com.ehuman.dao.service.impl;

import com.ehuman.dao.mapper.TbUserBotStatusDoMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ehuman.dao.entity.TbUserBotStatusDo;
import com.ehuman.dao.enums.MoodEnum;
import com.ehuman.dao.service.TbUserBotStatusDoService;
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

    @Override
    public void updateMoodByUserId(Long userId, MoodEnum newMood, String moodReason) {
        lambdaUpdate()
                .set(TbUserBotStatusDo::getMoodEnum, newMood)
                .eq(TbUserBotStatusDo::getUserId, userId)
                .set(TbUserBotStatusDo::getMoodReason, moodReason)
                .update();
    }
}




