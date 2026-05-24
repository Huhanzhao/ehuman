package com.helper.services.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.helper.services.entity.TbUserVoiceModelDo;
import com.helper.services.service.TbUserVoiceModelDoService;
import com.helper.services.mapper.TbUserVoiceModelDoMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author Lenovo
* @description 针对表【tb_user_voice_model】的数据库操作Service实现
* @createDate 2026-03-15 13:14:18
*/
@Service
public class TbUserVoiceModelDoServiceImpl extends ServiceImpl<TbUserVoiceModelDoMapper, TbUserVoiceModelDo>
    implements TbUserVoiceModelDoService{

    @Override
    public List<TbUserVoiceModelDo> getVoiceModelByUserId(Long userId) {
        return lambdaQuery()
                .eq(TbUserVoiceModelDo::getUserId, userId)
                .eq(TbUserVoiceModelDo::getEnable, true)
                .list();
    }

    @Override
    public List<TbUserVoiceModelDo> getByUserId(Long userId) {
        return lambdaQuery()
                .eq(TbUserVoiceModelDo::getUserId, userId)
                .list();
    }

    @Override
    public boolean switchVoiceModel(Long userId, String voiceModelName) {
        // 先查询是否有该模型
        TbUserVoiceModelDo targetModel = lambdaQuery()
                .eq(TbUserVoiceModelDo::getUserId, userId)
                .eq(TbUserVoiceModelDo::getVoiceModelName, voiceModelName)
                .one();
        if (targetModel == null) {
            return false;
        }
        // 把该用户所有模型设为不启用
        lambdaUpdate()
                .set(TbUserVoiceModelDo::getEnable, false)
                .eq(TbUserVoiceModelDo::getUserId, userId)
                .update();
        // 把目标模型设为启用
        lambdaUpdate()
                .set(TbUserVoiceModelDo::getEnable, true)
                .eq(TbUserVoiceModelDo::getId, targetModel.getId())
                .update();
        return true;
    }
}




