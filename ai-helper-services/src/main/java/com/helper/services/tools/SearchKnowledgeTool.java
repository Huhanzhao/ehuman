package com.helper.services.tools;

import com.helper.config.ChromaEmbeddingStoreFactory;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.service.V;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SearchKnowledgeTool {
    @Autowired
    private OpenAiEmbeddingModel embeddingModel;

    @Tool("矢量知识库中搜索问题相关知识")
    public EmbeddingSearchResult<TextSegment> searchKnowledge(@V("用户提问的消息") String userMessage){
        log.info("搜索知识库记录方法被调用了,用户提问：{}", userMessage);
        ChromaEmbeddingStore chromaEmbeddingStore =
                ChromaEmbeddingStoreFactory.getChromaEmbeddingStore("knowledge");

        Embedding embedding = embeddingModel.embed(userMessage).content();
        EmbeddingSearchRequest embeddingRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(4)
                .minScore(0.75)
                .build();

        EmbeddingSearchResult<TextSegment> search = chromaEmbeddingStore.search(embeddingRequest);
        log.info("知识库搜索结果：{}", search);
        return search;
    }
}
