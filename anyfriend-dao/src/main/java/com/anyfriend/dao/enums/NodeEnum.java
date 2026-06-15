package com.anyfriend.dao.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NodeEnum {

    DISABLE("disable",0),
    AUXILIARY("auxiliary",1),
    MASTER("master",2),
    IMAGE_ANALYSIS("ImageAnalysisNode",3),
    TTS("TTS",4);

    private final String nodeName;

    @EnumValue
    private final Integer nodeNum;

    public static String getByNameNodeNum(Integer nodeNum){
        for (NodeEnum value : values()) {
            if (nodeNum.equals(value.getNodeNum())){
                return value.getNodeName();
            }
        }
        return null;
    }
}
