package com.ehuman.dao.enums;


import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FeatureTypeEnum {

    VOICE_PLUGIN(1,"声音插件"),
    IMAGE_ANALYSIS(2,"图片解析"),
    EMOJI_FETCH(3,"自动解析表情包到表情包库");

    @EnumValue
    private final Integer featureType;

    private final String featureName;
}
