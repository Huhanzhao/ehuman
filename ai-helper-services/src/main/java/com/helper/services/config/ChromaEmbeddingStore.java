package com.helper.services.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.commonservicescommon.utils.ChromaUtils;
import com.helper.services.entity.ChromaSearchResponse;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ChromaEmbeddingStore implements EmbeddingStore<TextSegment> {

    @Autowired
    private ChromaUtils chromaUtils;


    @Override
    public String add(Embedding embedding) {
        // 暂时使用不到 没有元数据没法存文本
        return null;
    }

    @Override
    public void add(String s, Embedding embedding) {

    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        return null;
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest embeddingSearchRequest) {

        // 获取查询向量
        Embedding queryEmbedding = embeddingSearchRequest.queryEmbedding();
        float[] queryVector = queryEmbedding.vector();

        String block = chromaUtils.queryDataByVector("ai-helper", "ai-service",
                "knowledge", null, new float[][]{queryVector},embeddingSearchRequest.maxResults());

        ObjectMapper mapper = new ObjectMapper();
        ChromaSearchResponse resp = null;
        try {
            resp = mapper.readValue(block, ChromaSearchResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
        double minScore = embeddingSearchRequest.minScore();
        List<Double> distancesList = resp.getDistances().getFirst();
        List<String> documentsList = resp.getDocuments().getFirst();
        List<String> idsList = resp.getIds().getFirst();
        List<Map<String, Object>> metadataList = resp.getMetadatas().getFirst();
        for (int i = 0; i < resp.getDocuments().getFirst().size(); i++) {
            // 限制最小分数
            Double distance = distancesList.get(i);
            if (1 - distance > minScore) {
                EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(1-distance, idsList.get(i), null,
                        new TextSegment(documentsList.get(i), new Metadata(metadataList.get(i))));
                matches.add(match);
            }
        }
        return new EmbeddingSearchResult<>(matches);
    }


    @Override
    public List<String> addAll(List list) {
        return null;
    }
}