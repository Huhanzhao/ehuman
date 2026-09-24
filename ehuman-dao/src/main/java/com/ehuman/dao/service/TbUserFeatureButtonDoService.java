package com.ehuman.dao.service;

import com.ehuman.dao.entity.TbUserFeatureButtonDo;
import com.ehuman.dao.enums.FeatureTypeEnum;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Lenovo
* @description 针对表【tb_user_plugin】的数据库操作Service
* @createDate 2026-06-19 14:52:29
*/
public interface TbUserFeatureButtonDoService extends IService<TbUserFeatureButtonDo> {

    void turnOnOrOffFeature(Long userId, boolean enable, FeatureTypeEnum featureTypeEnum);

    boolean isEnableByUserId(Long userId, FeatureTypeEnum featureTypeEnum);

    void turnOnOrOffImageAnalyze(Long userId, boolean enable);
}
