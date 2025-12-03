package com.helper.user;

import com.helper.commonservicescommon.entity.AddChromaDataBody;
import com.helper.commonservicescommon.entity.ChromaDataMetaData;
import com.helper.commonservicescommon.utils.ChromaUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

@SpringBootTest
class AiHelperUserApplicationTests {

    @Autowired
    private ChromaUtils chromaUtils;

    @Test
    void contextLoads() {
        chromaUtils.isTenantExists("ai-helper");
    }
    @Test
    void createTenant() {
        chromaUtils.createTenant("ai-helper");
    }

    @Test
    void createDatabase() {
        chromaUtils.createDatabase("ai-helper", "ai-service");
    }

    @Test
    void createCollection() {
        Map<String,String> metadata = new HashMap<>();
        metadata.put("createBy","System");
        metadata.put("desc","AI 历史memory");
        metadata.put("createTime", DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        chromaUtils.createCollection("ai-helper", "ai-service","memory",metadata);
    }

    @Test
    void get() {
        String memory = chromaUtils.getCollectionUuid("ai-helper", "ai-service", "memory");
        System.out.println(memory);
    }

    @Test
    void add() {
        List<String> documents = List.of("小明是小红的哥哥");
        List<List<Double>> embeddings = List.of(List.of(0.11));
        List<String> ids = List.of(UUID.randomUUID().toString());
        List<String> uris = List.of("1");
        ChromaDataMetaData chromaDataMetaData = new ChromaDataMetaData();
        chromaDataMetaData.setUserId("admin");
        chromaDataMetaData.setSessionId("1");
        chromaDataMetaData.setCreateTime(DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        List<ChromaDataMetaData> metadatas = List.of(chromaDataMetaData);

        AddChromaDataBody addChromaDataBody = new AddChromaDataBody();
        addChromaDataBody.setDocuments(documents);
        addChromaDataBody.setMetadatas(metadatas);
        addChromaDataBody.setUris(uris);
        addChromaDataBody.setIds(ids);
        addChromaDataBody.setEmbeddings( embeddings);

        Boolean memory = chromaUtils.
                addChromaData("ai-helper", "ai-service",
                        "memory",addChromaDataBody);
        System.out.println(memory);
    }

}
