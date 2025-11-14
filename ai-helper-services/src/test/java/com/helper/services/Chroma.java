package com.helper.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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



    @Value("${chroma.memory-uuid}")
    private String chromaMemoryUuid;


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

        String block =  chromaWebClient.post()
                .uri("/api/v2/tenants/ai-helper/databases/memory/collections/" +
                        "1ef7ad24-08f7-4e96-baed-5b6848328164/query")
                .bodyValue(bodyMap)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        System.out.println(block);
    }
    @Test  // 正式测试
    void queryDataT() {
        // todo 用户上下文获取userId 根据userId 查询 记录的uuid
        String userId = "admin";

        Map<String,Object> bodyMap = new HashMap<>();
//        bodyMap.put("ids", new String[]{"admin"}); // 数据的ids 添加数据的时候定义 // 可以用来区分同一个用户的不同会话 目前不区分会话
        bodyMap.put("n_results",4); // 返回四条结果
        bodyMap.put("query_embeddings",new float[][]{new float[]{0.1f}});
        bodyMap.put("where",Map.of("userId",Map.of("$eq", userId)));  // 过滤元数据中的用户id

        String block =  chromaWebClient.post()
                .uri("/api/v2/tenants/ai-helper/databases/memory/collections/" +
                        chromaMemoryUuid +
                        "/query")  // 集合的uuid 集合名查询chroma得到 可以用来区分用户
                .bodyValue(bodyMap)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        System.out.println(block);
    }

}
