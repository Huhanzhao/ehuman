package com.bot.service.service;

import com.bot.service.entity.TbUserVoiceModelDo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author Lenovo
* @description 针对表【tb_user_voice_model】的数据库操作Service
* @createDate 2026-03-15 13:14:18
*/
public interface TbUserVoiceModelDoService extends IService<TbUserVoiceModelDo> {

    List<TbUserVoiceModelDo> getVoiceModelByUserId(Long userId);

    List<TbUserVoiceModelDo> getByUserId(Long userId);

    boolean switchVoiceModel(Long userId, String voiceModelName);
}
