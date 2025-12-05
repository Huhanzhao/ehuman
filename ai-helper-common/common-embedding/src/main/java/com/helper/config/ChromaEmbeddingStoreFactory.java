package com.helper.config;

import dev.langchain4j.store.embedding.chroma.ChromaApiVersion;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;

public class ChromaEmbeddingStoreFactory {

    public static ChromaEmbeddingStore getChromaEmbeddingStore(String collections){

        return ChromaEmbeddingStore.builder()
                .baseUrl("http://localhost:8000")
                .apiVersion(ChromaApiVersion.V2)
                .tenantName("ai-helper")
                .databaseName("ai-service")
                .collectionName(collections)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
