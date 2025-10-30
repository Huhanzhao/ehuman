package com.ai.langchai4j.aihelper.aistudy;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AIServiceRAG {

    @Autowired
    private OpenAiChatModel openAiChatModel;

    @Test
    void RAG(){

//        EmbeddingStore embeddingStore  = ...
//        EmbeddingModel embeddingModel = ...
//
//        ContentRetriever contentRetriever = new EmbeddingStoreContentRetriever(embeddingStore, embeddingModel);
//
//        AIServiceTest aiServiceTest = AiServices.builder(AIServiceTest.class)
//                .chatModel(openAiChatModel)
//                .contentRetriever(contentRetriever)
//                .build();
    }


    @Test
    void RAG2(){
//        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
//                .queryTransformer(...)
//        .queryRouter(...)
//        .contentAggregator(...)
//        .contentInjector(...)
//        .executor(...)
//        .build();
//
//        AIServiceTest aiServiceTest = AiServices.builder(AIServiceTest.class)
//                .chatModel(openAiChatModel)
//                .retrievalAugmentor(retrievalAugmentor)
//                .build();
    }


}
