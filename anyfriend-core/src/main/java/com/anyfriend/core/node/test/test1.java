package com.anyfriend.core.node.test;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class test1 implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        List<String> aaaa = (List)state.value("test").orElse(null);
        log.info("test: {}", aaaa);
        return Map.of();
    }

}
