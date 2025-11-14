package com.helper.services.service.impl;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class ChromaEmbeddingStore implements EmbeddingStore {

    private final WebClient chromaWebClient;

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
        return null;
//        // 1. 获取查询向量
//        Embedding queryEmbedding = embeddingSearchRequest.queryEmbedding();
//        float[] queryVector = queryEmbedding.vector(); // 假设有 vector() 方法
//
//        // 2. 构建请求体
//        Map<String, Object> body = new HashMap<>();
//        body.put("collection_name", "chat_records");       // 集合名
//        body.put("query_embeddings", List.of(queryVector)); // Chroma 支持批量查询，这里只查一个
//        body.put("n_results", embeddingSearchRequest.topK());
//
//        // 如果有 filter
//        if (embeddingSearchRequest.filter() != null) {
//            body.put("where", embeddingSearchRequest.filter()); // 假设是 Map<String,Object>
//        }
//
//        // 3. 调用 Chroma HTTP API
//        String jsonResponse = chromaWebClient.post()
//                .uri("/collections/{name}/query", "chat_records")
//                .bodyValue(body)
//                .retrieve()
//                .bodyToMono(String.class)
//                .block(); // 同步调用
//
//        // 4. 解析返回 JSON，转换成 EmbeddingSearchResult
//        // Chroma 返回格式示例：
//        // {
//        //   "ids": ["id1","id2"],
//        //   "distances": [0.1,0.3],
//        //   "documents": ["文本1","文本2"],
//        //   "metadatas": [{"userId":"123"}, {"userId":"456"}]
//        // }
//        return parseChromaJsonToEmbeddingSearchResult(jsonResponse);
    }

//    private EmbeddingSearchResult parseChromaJsonToEmbeddingSearchResult(String json) {
//        // 这里使用你的 JSON 库解析
//        JSONObject obj = new JSONObject(json);
//        List<String> ids = new ArrayList<>();
//        List<Double> distances = new ArrayList<>();
//        List<String> documents = new ArrayList<>();
//        List<Map<String, Object>> metadatas = new ArrayList<>();
//
//        JSONArray idsArray = obj.getJSONArray("ids");
//        JSONArray distancesArray = obj.getJSONArray("distances");
//        JSONArray docsArray = obj.getJSONArray("documents");
//        JSONArray metasArray = obj.getJSONArray("metadatas");
//
//        for (int i = 0; i < idsArray.length(); i++) {
//            ids.add(idsArray.getString(i));
//            distances.add(distancesArray.getDouble(i));
//            documents.add(docsArray.getString(i));
//            metadatas.add(metasArray.getJSONObject(i).toMap());
//        }
//
//        return EmbeddingSearchResult.builder()
//                .ids(ids)
//                .distances(distances)
//                .documents(documents)
//                .metadatas(metadatas)
//                .build();
//    }

    @Override
    public List<String> addAll(List list) {
        return null;
    }
}