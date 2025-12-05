//package com.helper.services;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.helper.commonservicescommon.entity.AddChromaDataBody;
//import com.helper.commonservicescommon.entity.ChromaDataMetaData;
//import com.helper.commonservicescommon.utils.ChromaUtils;
//import dev.langchain4j.data.embedding.Embedding;
//import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.time.DateFormatUtils;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.util.Date;
//import java.util.List;
//import java.util.UUID;
//
//@SpringBootTest
//@Slf4j
//public class AppTest {
//
//    @Autowired
//    private ChromaUtils chromaUtils;
//
//    @Autowired
//    private OpenAiEmbeddingModel embeddingModel;
//
//    @Test
//    void query() {
//        String block = chromaUtils.
//                queryDataByVector("ai-helper", "ai-service",
//                        "memory",null,new float[][]{new float[]{0.1f}},5);
//        log.info("查询结果：{}", block);
//        ObjectMapper mapper = new ObjectMapper();
//        ChromaSearchResponse resp = null;
//        try {
//            resp = mapper.readValue(block, ChromaSearchResponse.class);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//        System.out.println(block);
//    }
//
//    @Test
//    void add() {
//        List<String> documents = List.of("西瓜是红色的");
//        Embedding embedding = embeddingModel.embed("西瓜是红色的").content();
//        float[][] embeddings = new float[][]{embedding.vector()};
//        List<String> ids = List.of(UUID.randomUUID().toString());
//        List<String> uris = List.of("1");
//        ChromaDataMetaData chromaDataMetaData = new ChromaDataMetaData();
//        chromaDataMetaData.setUserId("admin");
//        chromaDataMetaData.setSessionId("1");
//        chromaDataMetaData.setRole("user");
//        chromaDataMetaData.setCreateTime(DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
//        List<ChromaDataMetaData> metadatas = List.of(chromaDataMetaData);
//
//        AddChromaDataBody addChromaDataBody = new AddChromaDataBody();
//        addChromaDataBody.setDocuments(documents);
//        addChromaDataBody.setMetadatas(metadatas);
//        addChromaDataBody.setUris(uris);
//        addChromaDataBody.setIds(ids);
//        addChromaDataBody.setEmbeddings( embeddings);
//
//        Boolean memory = chromaUtils.
//                addChromaData("ai-helper", "ai-service",
//                        "memory",addChromaDataBody);
//        System.out.println(memory);
//    }
//}
