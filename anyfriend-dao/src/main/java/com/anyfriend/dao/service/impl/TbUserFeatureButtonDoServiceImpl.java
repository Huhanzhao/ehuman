package com.anyfriend.dao.service.impl;

import com.anyfriend.dao.entity.TbUserFeatureButtonDo;
import com.anyfriend.dao.mapper.TbUserFeatureButtonDoMapper;
import com.anyfriend.dao.service.TbUserFeatureButtonDoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.anyfriend.dao.enums.FeatureTypeEnum.VOICE_PLUGIN;

/**
* @author Lenovo
* @description 针对表【tb_user_feature_button】的数据库操作Service实现
* @createDate 2026-06-19 14:52:29
*/
@Service
public class TbUserFeatureButtonDoServiceImpl extends ServiceImpl<TbUserFeatureButtonDoMapper, TbUserFeatureButtonDo>
    implements TbUserFeatureButtonDoService {

    @Override
    @Transactional
    public void turnOnOrOffTTSPlugin(Long userId, boolean enable) {
        boolean exists = lambdaQuery().eq(TbUserFeatureButtonDo::getUserId, userId).exists();
        if (!exists) {
            TbUserFeatureButtonDo tbUserFeatureButtonDo = new TbUserFeatureButtonDo();
            tbUserFeatureButtonDo.setUserId(userId);
            tbUserFeatureButtonDo.setFeatureTypeEnum(VOICE_PLUGIN);
            tbUserFeatureButtonDo.setEnable(enable);
            save(tbUserFeatureButtonDo);
        }else{
            lambdaUpdate().set(TbUserFeatureButtonDo::getEnable,enable).eq(TbUserFeatureButtonDo::getUserId, userId).update();
        }
    }

    @Override
    public boolean isEnableByUserId(Long userId) {
        return lambdaQuery().eq(TbUserFeatureButtonDo::getUserId, userId)
                .eq(TbUserFeatureButtonDo::getFeatureTypeEnum, VOICE_PLUGIN)
                .eq(TbUserFeatureButtonDo::getEnable, true).exists();
    }
}




