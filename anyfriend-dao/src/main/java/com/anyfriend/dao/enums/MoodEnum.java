package com.anyfriend.dao.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MoodEnum {

    CALM(0, "平静"),
    HAPPY(1, "开心"),
    SAD(2, "悲伤"),
    ANGRY(3, "愤怒"),
    IRRITABLE(4, "烦躁");

    @EnumValue
    private final int code;
    private final String desc;

    public static MoodEnum fromCode(int code) {
        for (MoodEnum mood : values()) {
            if (mood.code == code) {
                return mood;
            }
        }
        return CALM;
    }
}
