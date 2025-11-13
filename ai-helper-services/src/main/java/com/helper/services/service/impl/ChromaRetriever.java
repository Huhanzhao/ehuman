import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ChromaEmbeddingStore implements EmbeddingStore {

    private final WebClient webClient;

    public ChromaEmbeddingStore(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public void addDocuments(List<Document> documents, EmbeddingModel embeddingModel) {
        // 1. 先用 embeddingModel 生成向量
        List<double[]> embeddings = documents.stream()
                .map(doc -> embeddingModel.embed(doc.text()))
                .collect(Collectors.toList());

        // 2. 调用 Chroma HTTP API 插入
        for (int i = 0; i < documents.size(); i++) {
            Map<String, Object> body = Map.of(
                    "collection_name", "chat_records",
                    "documents", List.of(documents.get(i).text()),
                    "embeddings", List.of(embeddings.get(i)),
                    "metadatas", List.of(documents.get(i).metadata())
            );

            webClient.post()
                    .uri("/collections/{name}/add", "chat_records")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        }
    }

    @Override
    public List<Document> similaritySearch(String query, EmbeddingModel embeddingModel, int topK) {
        double[] queryVector = embeddingModel.embed(query);

        Map<String, Object> body = Map.of(
                "collection_name", "chat_records",
                "query_embeddings", List.of(queryVector),
                "n_results", topK
        );

        String json = webClient.post()
                .uri("/collections/{name}/query", "chat_records")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // TODO: 解析 JSON -> List<Document>
        return parseChromaJsonToDocuments(json);
    }
}