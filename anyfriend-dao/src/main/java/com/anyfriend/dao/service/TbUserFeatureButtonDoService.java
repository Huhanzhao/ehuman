package com.anyfriend.dao.service;

import com.anyfriend.dao.entity.TbUserFeatureButtonDo;
import com.anyfriend.dao.enums.FeatureTypeEnum;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Lenovo
* @description 针对表【tb_user_plugin】的数据库操作Service
* @createDate 2026-06-19 14:52:29
*/
public interface TbUserFeatureButtonDoService extends IService<TbUserFeatureButtonDo> {

    void turnOnOrOffFeature(Long userId, boolean b, FeatureTypeEnum featureTypeEnum);

    boolean isEnableByUserId(Long userId, FeatureTypeEnum featureTypeEnum);
}
