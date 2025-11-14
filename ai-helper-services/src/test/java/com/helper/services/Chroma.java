package com.helper.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class Chroma {
    // chroma 使用docker拉去镜像之后 默认端口8000/docs 是接口文档

    @Autowired
    private WebClient.Builder webClientBuilder;

    private WebClient chromaWebClient;

    @BeforeEach
    void setUp() {
        chromaWebClient = webClientBuilder.baseUrl("http://localhost:8000").build();
    }


    // 创建一个collection
    @Test
    void createCollection() {
        String block = chromaWebClient.post()
                .uri("/api/v2/tenants/ai-helper/databases/memory/collections")
                .bodyValue(Map.of("name", "admin"))
                .retrieve()
                .bodyToMono(String.class)
                .block();
        System.out.println(block);
    }

    // 查询数据
    @Test
    void queryData() {
        Map<String,Object> bodyMap = new HashMap<>();
        double[][] queryVector = new double[][]{new double[]{0.1}};
        bodyMap.put("ids", new String[]{"admin"});
        bodyMap.put("n_results",10);
        bodyMap.put("query_embeddings",queryVector);
        bodyMap.put("where_document",Map.of("$contains","你")); // 文档中包含等命令  更多见 https://cookbook.chromadb.dev/core
        bodyMap.put("where",Map.of("name",Map.of("$eq", "第一条数据"))); // meta元数据中包含等命令

        String string = bodyMap.toString();
        System.out.println("======================" + string);

        WebClient.RequestHeadersSpec<?> requestHeadersSpec = chromaWebClient.post()
                .uri("/api/v2/tenants/ai-helper/databases/memory/collections/" +
                        "1ef7ad24-08f7-4e96-baed-5b6848328164/query")
                .bodyValue(bodyMap);

        String block = requestHeadersSpec
                .retrieve()
                .bodyToMono(String.class)
                .block();
        System.out.println(block);
    }

}
