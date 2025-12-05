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
import dev.langchain4j.store.embedding.filter.Filter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Component
@Slf4j
public class SearchOldMemoryTool {

    @Autowired
    private OpenAiEmbeddingModel embeddingModel;

    @Tool("矢量库中搜索问题相关历史聊天记录")
    public EmbeddingSearchResult<TextSegment> searchChatMessage(@V("用户提问的消息") String userMessage, @V("该参数是用户的 userId，需要传userId，不能传其他的")String userId){
        log.info("搜索历史聊天记录方法被调用了,用户提问：{},用户记忆id：{}", userMessage,userId);
        ChromaEmbeddingStore chromaEmbeddingStore =
                ChromaEmbeddingStoreFactory.getChromaEmbeddingStore("memory");

        // 过滤器
        Filter filter = metadataKey("userId").isEqualTo(userId);

        Embedding embedding = embeddingModel.embed(userMessage).content();
        EmbeddingSearchRequest embeddingRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(4)
                .filter(filter)
                .minScore(0.75)
                .build();

        EmbeddingSearchResult<TextSegment> search = chromaEmbeddingStore.search(embeddingRequest);
        log.info("知识库搜索结果：{}", search);
        return search;
    }
}
