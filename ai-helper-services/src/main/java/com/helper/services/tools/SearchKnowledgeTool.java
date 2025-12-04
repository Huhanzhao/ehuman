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
public class SearchKnowledgeTool {
    @Autowired
    private ChromaUtils chromaUtils;

    @Autowired
    private OpenAiEmbeddingModel embeddingModel;

    @Tool("矢量知识库中搜索问题相关知识")
    public String searchKnowledge(@V("用户提问的消息") String userMessage){
        log.info("搜索知识库记录方法被调用了,用户提问：{}", userMessage);
        Embedding embedding = embeddingModel.embed(userMessage).content();
        String block = chromaUtils.queryDataByVector("ai-helper", "ai-service",
                "knowledge", null, new float[][]{embedding.vector()}, 5);

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
        log.info("知识库搜索结果：{}", textSegments);
        return textSegments.toString();
    }
}
