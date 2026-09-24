package com.ehuman.dao.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FamiliarityEnum {

    STRANGER(0, 39, "陌生"),
    NORMAL(40, 79, "普通"),
    FAMILIAR(80, 119, "熟悉"),
    CLOSE(120, 159, "亲密"),
    BELOVED(160, 200, "喜爱");

    private final int min;
    private final int max;
    private final String desc;

    public static FamiliarityEnum fromScore(int score) {
        for (FamiliarityEnum f : values()) {
            if (score >= f.min && score <= f.max) {
                return f;
            }
        }
        return STRANGER;
    }
}
