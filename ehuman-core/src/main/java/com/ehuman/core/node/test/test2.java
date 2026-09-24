package com.ehuman.core.node.test;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class test2 implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
//        state.input(Map.of("test", "abc"));
        return Map.of("test", "abc");
    }

}
