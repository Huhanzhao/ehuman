package com.anyfriend.dao.service.impl;

import com.anyfriend.dao.entity.TbUserFeatureButtonDo;
import com.anyfriend.dao.enums.FeatureTypeEnum;
import com.anyfriend.dao.mapper.TbUserFeatureButtonDoMapper;
import com.anyfriend.dao.service.TbUserFeatureButtonDoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public void turnOnOrOffFeature(Long userId, boolean enable, FeatureTypeEnum featureTypeEnum) {
        boolean exists = lambdaQuery().eq(TbUserFeatureButtonDo::getUserId, userId).exists();
        if (!exists) {
            TbUserFeatureButtonDo tbUserFeatureButtonDo = new TbUserFeatureButtonDo();
            tbUserFeatureButtonDo.setUserId(userId);
            tbUserFeatureButtonDo.setFeatureTypeEnum(featureTypeEnum);
            tbUserFeatureButtonDo.setEnable(enable);
            save(tbUserFeatureButtonDo);
        }else{
            lambdaUpdate().set(TbUserFeatureButtonDo::getEnable,enable).eq(TbUserFeatureButtonDo::getUserId, userId).update();
        }
    }

    @Override
    public boolean isEnableByUserId(Long userId, FeatureTypeEnum featureTypeEnum) {
        return lambdaQuery().eq(TbUserFeatureButtonDo::getUserId, userId)
                .eq(TbUserFeatureButtonDo::getFeatureTypeEnum, featureTypeEnum)
                .eq(TbUserFeatureButtonDo::getEnable, true).exists();
    }

    @Override
    @Transactional
    public void turnOnOrOffImageAnalyze(Long userId, boolean enable) {
        if (enable){
            this.turnOnOrOffFeature(userId, true, FeatureTypeEnum.IMAGE_ANALYSIS);
        }else{
            this.turnOnOrOffFeature(userId, false, FeatureTypeEnum.IMAGE_ANALYSIS);
            this.turnOnOrOffFeature(userId, false, FeatureTypeEnum.EMOJI_FETCH);
        }
    }
}




