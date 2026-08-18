package com.anyfriend.core.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.anyfriend.core.service.EmbeddingService;
import com.anyfriend.dao.entity.TbMessageVectorSearchDo;
import com.anyfriend.dao.service.DBVectorOperationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 类的描述：向量记忆检索node
 * 作者：huhanzhao
 * 创建时间：2026/8/10
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class MemoryVectorSearchNode implements NodeAction {

    private final DBVectorOperationService dbVectorOperationService;

    private final EmbeddingService embeddingService;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Long userId = state.value("userId", Long.class).orElse(0L);
        String userInput = state.value("userInput", String.class).orElse("");

        if (userInput.isEmpty()) return Map.of();

        // 调用向量模型 向量化问题
        float[] floats = embeddingService.embeddingText(userId, userInput);
        if (floats == null){
            log.info("没有向量模型跳过");
            return Map.of();
        }

        // 检索记忆
        List<TbMessageVectorSearchDo> tbMessageVectorSearchDos = dbVectorOperationService.searchVector(userId,
                Arrays.toString(floats),
                2,
                0.8);

        // 修改检索记忆的格式
        StringBuilder stringBuilder = new StringBuilder();
        for (TbMessageVectorSearchDo tbMessageVectorSearchDo : tbMessageVectorSearchDos) {
            stringBuilder.append("时间：").append(tbMessageVectorSearchDo.getCreateTime()).append(" ")
                    .append(tbMessageVectorSearchDo.getContext()).append("\n");
        }
        return Map.of("userInput", userInput, "vectorMemory", stringBuilder.toString());
    }
}
