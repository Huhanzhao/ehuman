package com.helper.services.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helper.services.entity.ChromaSearchResponse;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ChromaEmbeddingStore implements EmbeddingStore {

    private final WebClient chromaWebClient;

    @Value("chroma.memory.uuid")
    private String chromaMemoryUuid;

    public ChromaEmbeddingStore(WebClient.Builder webClientBuilder) {
        // 指定 baseUrl
        this.chromaWebClient = webClientBuilder.baseUrl("http://localhost:8000").build();
    }

    @Override
    public String add(Embedding embedding) {
        // 暂时使用不到 没有元数据没法存文本
        return null;
    }

    @Override
    public void add(String s, Embedding embedding) {

    }

    @Override
    public String add(Embedding embedding, Object o) {
        return null;
    }

    @Override
    public EmbeddingSearchResult search(EmbeddingSearchRequest embeddingSearchRequest) {

        // 获取查询向量
        Embedding queryEmbedding = embeddingSearchRequest.queryEmbedding();
        float[] queryVector = queryEmbedding.vector();

        // 获取最小分数 转换成距离
//        double minScore = embeddingSearchRequest.minScore();
//        double distance = 1 - minScore;

        // todo 用户上下文获取userId 根据userId 查询 记录的uuid
        String userId = "admin";

        Map<String,Object> bodyMap = new HashMap<>();
//        bodyMap.put("ids", new String[]{"admin"}); // 数据的ids 添加数据的时候定义 // 可以用来区分同一个用户的不同会话 目前不区分会话
        bodyMap.put("n_results",4); // 返回四条结果
        bodyMap.put("query_embeddings",new float[][]{queryVector});
        bodyMap.put("where",Map.of("userId",Map.of("$eq", userId)));  // 过滤元数据中的用户id

        String block =  chromaWebClient.post()
                .uri("/api/v2/tenants/ai-helper/databases/memory/collections/" +
                        chromaMemoryUuid +
                        "/query")  // 集合的uuid 集合名查询chroma得到 可以用来区分用户
                .bodyValue(bodyMap)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        //        {"ids":[["admin","admin2"]],"embeddings":null,"documents":[["你好我叫小明","你好我是机器人"]],
//            "uris":null,"metadatas":[[{"userId":"admin","additionalProp1":true},
//            {"role":"ai","userId":"admin"}]],"distances":[[0.00039999984,0.00039999984]],
//            "include":["documents","metadatas","distances"]}

        ObjectMapper mapper = new ObjectMapper();
        ChromaSearchResponse resp = null;
        try {
            resp = mapper.readValue(block, ChromaSearchResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        List<EmbeddingMatch<String>> matches = new ArrayList<>();
        for (int i = 0; i < resp.getDocuments().size(); i++) {
            String doc = resp.documents.get(0).get(i);

            Map<String, Object> metadata = resp.metadatas != null && !resp.metadatas.isEmpty()
                    ? resp.metadatas.get(0).get(i)
                    : null;

            Double distance = resp.distances != null && !resp.distances.isEmpty()
                    ? resp.distances.get(0).get(i)
                    : null;

            String embeddingId = resp.ids != null && !resp.ids.isEmpty()
                    ? resp.ids.get(0).get(i)
                    : null;

            // todo <string> 改成自定义类包含metadata
            EmbeddingMatch<String> match = new EmbeddingMatch<>(distance != null ? 1.0 - distance : null,embeddingId,null, doc);

            matches.add(match);
        }

        return new EmbeddingSearchResult<>(matches);
    }


    @Override
    public List<String> addAll(List list) {
        return null;
    }
}