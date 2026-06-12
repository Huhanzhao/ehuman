package com.anyfriend.dao.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NodeEnum {

    DISABLE("disable",0),
    WANT_TO("UserWantToNode",1),
    MOOD("BotStatusSearchNode",2),
    GENERATE("MessageGenerateNode",3),
    IMAGE_ANALYSIS("ImageAnalysisNode",4),
    TTS("TTS",9999);

    public static NodeEnum fromCode(Integer nodeNum){
        for (NodeEnum value : values()) {
            if (nodeNum.equals(value.getNodeNum())){
                return value;
            }
        }
        return null;
    }

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
