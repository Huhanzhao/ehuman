package com.anyfriend.dao.service;

import com.anyfriend.dao.entity.TbUserReferAudioDo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author Lenovo
* @description 针对表【tb_user_refer_audio】的数据库操作Service
* @createDate 2026-03-15 13:14:18
*/
public interface TbUserReferAudioDoService extends IService<TbUserReferAudioDo> {

    List<TbUserReferAudioDo> getReferAudioByUserId(Long userId, Boolean enable);

    List<TbUserReferAudioDo> getByUserId(Long userId);

    boolean switchVoiceModel(Long userId, String voiceModelName);

    List<TbUserReferAudioDo> getDisableReferAudioByUserIdAndType(Long userId, String type);

    void changeReferAudio(Long userId, TbUserReferAudioDo tbUserReferAudioDo);
}
