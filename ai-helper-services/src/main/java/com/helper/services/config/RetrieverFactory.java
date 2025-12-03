package com.helper.services.config;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocuments;

@Component
public class RetrieverFactory {

    @Autowired
    private OpenAiEmbeddingModel embeddingModel;

    @Autowired
    private ChromaEmbeddingStore chromaEmbeddingStore;

    void EasyRAG(){

        // todo 存到矢量库 逻辑
        // 创建过滤器
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:*资料.txt");
        // 获取文档
        List<Document> documents = loadDocuments("D:\\project\\ideaworkspace\\ai-helper\\src\\main\\resources",pathMatcher);
//        List<Document> documents = loadDocuments(toPath("documents/"), glob("*.txt"));

        // 2️⃣ 使用文档分割器，把每个 Document 切成小块（每块500字符，重叠50）
//        RecursiveDocumentSplitter splitter = new RecursiveDocumentSplitter(500, 50);
        DocumentByParagraphSplitter documentByParagraphSplitter = new DocumentByParagraphSplitter(500, 50);
        // 智能切割器
//        DocumentSplitter splitter = DocumentSplitters.recursive(500, 50);



        // 内存的矢量库
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(documentByParagraphSplitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
        ingestor.ingest(documents);

//        todo 创建一个可直接注入的 持久化矢量库

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(chromaEmbeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                // maxResults 也可以根据查询动态指定
//                .dynamicMaxResults(query -> 3)
                .minScore(0.75)
                // minScore 也可以根据查询动态指定
//                .dynamicMinScore(query -> 0.75)
//                .filter(metadataKey("userId").isEqualTo("12345"))
                // filter 也可以根据查询动态指定
//                .dynamicFilter(query -> {
//                    String userId = getUserId(query.metadata().chatMemoryId());
//                    return metadataKey("userId").isEqualTo(userId);
//                }
//                )
                .build();
    }
}
