package com.anyfriend.dao.service.impl;

import com.anyfriend.dao.mapper.TbUserReferAudioDoMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anyfriend.dao.entity.TbUserReferAudioDo;
import com.anyfriend.dao.service.TbUserReferAudioDoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
* @author Lenovo
* @description 针对表【tb_user_refer_audio】的数据库操作Service实现
* @createDate 2026-03-15 13:14:18
*/
@Service
public class TbUserReferAudioDoServiceImpl extends ServiceImpl<TbUserReferAudioDoMapper, TbUserReferAudioDo>
    implements TbUserReferAudioDoService {

    @Override
    public List<TbUserReferAudioDo> getReferAudioByUserId(Long userId, Boolean enable) {
        return lambdaQuery()
                .eq(TbUserReferAudioDo::getUserId, userId)
                .eq(TbUserReferAudioDo::getEnable, enable)
                .list();
    }

    @Override
    public List<TbUserReferAudioDo> getByUserId(Long userId) {
        return lambdaQuery()
                .eq(TbUserReferAudioDo::getUserId, userId)
                .list();
    }

    @Override
    public boolean switchVoiceModel(Long userId, String referAudioName) {
        // 先查询是否有该模型
        TbUserReferAudioDo targetModel = lambdaQuery()
                .eq(TbUserReferAudioDo::getUserId, userId)
                .eq(TbUserReferAudioDo::getReferAudioName, referAudioName)
                .one();
        if (targetModel == null) {
            return false;
        }
        // 把该用户所有模型设为不启用
        lambdaUpdate()
                .set(TbUserReferAudioDo::getEnable, false)
                .eq(TbUserReferAudioDo::getUserId, userId)
                .update();
        // 把目标模型设为启用
        lambdaUpdate()
                .set(TbUserReferAudioDo::getEnable, true)
                .eq(TbUserReferAudioDo::getId, targetModel.getId())
                .update();
        return true;
    }

    @Override
    public List<TbUserReferAudioDo> getDisableReferAudioByUserIdAndType(Long userId, String type) {
        return lambdaQuery().eq(TbUserReferAudioDo::getType,type).eq(TbUserReferAudioDo::getUserId,userId)
                .eq(TbUserReferAudioDo::getEnable,false).orderByAsc(TbUserReferAudioDo::getId).list();
    }

    @Override
    @Transactional
    public void changeReferAudio(Long userId, TbUserReferAudioDo tbUserReferAudioDo) {
        lambdaUpdate().eq(TbUserReferAudioDo::getUserId, userId).set(TbUserReferAudioDo::getEnable, false).update();
        
        lambdaUpdate().update(tbUserReferAudioDo);
    }
}




