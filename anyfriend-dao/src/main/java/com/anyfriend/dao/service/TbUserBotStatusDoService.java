package com.anyfriend.dao.service;

import com.anyfriend.dao.entity.TbUserBotStatusDo;
import com.anyfriend.dao.enums.MoodEnum;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Lenovo
* @description 针对表【tb_user_bot_status】的数据库操作Service
* @createDate 2026-01-25 15:37:28
*/
public interface TbUserBotStatusDoService extends IService<TbUserBotStatusDo> {

    TbUserBotStatusDo getByUserId(Long userId);

    void updateGraceByUserId(Long userId, Integer grace);

    void updateFamiliarityByUserId(Long userId, Integer familiarity);

    void updateMoodByUserId(Long userId, MoodEnum newMood, String reason);
}
