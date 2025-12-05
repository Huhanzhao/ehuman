package com.helper.services.config;

import com.helper.config.ChromaEmbeddingStoreFactory;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class RetrieverFactory {

    @Autowired
    private OpenAiEmbeddingModel embeddingModel;

    @Bean
    public ContentRetriever getContentRetriever(){
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(ChromaEmbeddingStoreFactory.getChromaEmbeddingStore("knowledge"))
                .embeddingModel(embeddingModel)
                .maxResults(3)
                // maxResults 也可以根据查询动态指定
                // .dynamicMaxResults(query -> 3)
                .minScore(0.75)
                // minScore 也可以根据查询动态指定
                // .dynamicMinScore(query -> 0.75)
                // .filter(metadataKey("userId").isEqualTo("12345"))
                // filter 也可以根据查询动态指定
                // .dynamicFilter(query -> {
                //      String userId = getUserId(query.metadata().chatMemoryId());
                //      return metadataKey("userId").isEqualTo(userId);
                //             }
                //                )
                .build();
    }
}
