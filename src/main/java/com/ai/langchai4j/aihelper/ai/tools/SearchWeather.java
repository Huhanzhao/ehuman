package com.ai.langchai4j.aihelper.ai.tools;

import dev.langchain4j.agent.tool.Tool;
import io.swagger.v3.oas.annotations.Parameter;

public class SearchWeather {

    @Tool
    public String searchWeather(@Parameter(description = "城市名称") String city) {
        // 模拟调用天气查询接口，返回天气信息
        return "天气查询结果：" + city + "今天天气晴朗，温度23度";
    }
}
