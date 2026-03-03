package com.helper.services.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NodeEnum {

    WANT_TO("UserWantToNode",1),
    GRACE("UserGracesNode",2),
    GENERATE("MessageGenerateNode",3),
    IMAGE_ANALYSIS("ImageAnalysisNode",4);



    private final String nodeName;
    private final Integer nodeNum;
}
