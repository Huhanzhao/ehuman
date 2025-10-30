package com.ai.langchai4j.aihelper.aistudy.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

public class SearchWeather {

    @Tool(name = "查询天气",value = "根据城市的名称查询天气")
    public String searchWeather(@P("应该返回的天气的城市名称") String city) {
        // 模拟调用天气查询接口，返回天气信息
        return "天气查询结果：" + city + "今天天气晴朗，温度23度";
    }

    @Tool("计算两数之和")
    int add(int a, int b) {
        System.out.println("调用了加方法");
        return a + b;
    }

    @Tool("计算两数乘积")
    int multiply(int a, int b) {
        System.out.println("调用了乘法方法");
        return a * b;
    }
}
