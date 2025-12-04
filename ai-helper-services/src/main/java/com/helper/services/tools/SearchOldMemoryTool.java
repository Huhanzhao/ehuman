package com.helper.services.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.commonservicescommon.utils.ChromaUtils;
import com.helper.services.entity.ChromaSearchResponse;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.service.V;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class SearchOldMemoryTool {
    @Autowired
    private ChromaUtils chromaUtils;

    @Autowired
    private OpenAiEmbeddingModel embeddingModel;

    @Tool("矢量库中搜索问题相关历史聊天记录")
    public String searchChatMessage(@V("用户提问的消息") String userMessage, @V("该参数是用户的 userId，需要传userId，不能传其他的")String userId){
        log.info("搜索历史聊天记录方法被调用了,用户提问：{},用户记忆id：{}", userMessage,userId);
        Embedding embedding = embeddingModel.embed(userMessage).content();
        String block = chromaUtils.queryDataByVector("ai-helper", "ai-service",
                "memory", Map.of("userId", userId), new float[][]{embedding.vector()}, 5);

        ObjectMapper mapper = new ObjectMapper();
        ChromaSearchResponse resp = null;
        try {
            resp = mapper.readValue(block, ChromaSearchResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        List<TextSegment> textSegments = new ArrayList<>();
        List<String> documentsList = resp.getDocuments().getFirst();
        List<Map<String, Object>> metadataList = resp.getMetadatas().getFirst();
        for (int i = 0; i < resp.getDocuments().size(); i++){
            textSegments.add(new TextSegment(documentsList.get(i), new Metadata(metadataList.get(i))));
        }
        log.info("搜索结果：{}", textSegments);
        return textSegments.toString();
    }
}
