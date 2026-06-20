package com.anyfriend.dao.enums;


import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FeatureTypeEnum {

    VOICE_PLUGIN(1,"声音插件");

    @EnumValue
    private final Integer featureType;

    private final String featureName;
}
