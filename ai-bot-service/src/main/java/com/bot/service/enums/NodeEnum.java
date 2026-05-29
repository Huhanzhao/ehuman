package com.bot.service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NodeEnum {

    WANT_TO("UserWantToNode",1),
    MOOD("BotStatusSearchNode",2),
    GENERATE("MessageGenerateNode",3),
    IMAGE_ANALYSIS("ImageAnalysisNode",4);



    private final String nodeName;
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
