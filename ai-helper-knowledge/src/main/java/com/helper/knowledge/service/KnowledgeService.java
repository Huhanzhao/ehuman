package com.helper.knowledge.service;

import com.helper.commonservicescommon.entity.BusinessException;
import com.helper.commonservicescommon.entity.Result;
import com.helper.commonservicescommon.utils.UserContext;
import com.helper.knowledge.entity.TbKnowledge;
import com.helper.knowledge.utils.ReadFilesUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

@Service
@Slf4j
public class KnowledgeService {

    @Autowired
    private MinioService minioService;

    @Autowired
    private TbKnowledgeService tbKnowledgeService;

    @Autowired
    private OpenAiEmbeddingModel openAiEmbeddingModel;

//    @Autowired
//    private ChromaUtils chromaUtils;

    public void downloadFile(String fileName, HttpServletResponse response){
        try (
                InputStream inputStream = minioService.downloadFile(fileName,"ai-helper-knowledge")) {
            // 设置内容类型
            response.setContentType("application/octet-stream");

            // 设置下载文件名，并处理中文
            String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

            // 写出流
            inputStream.transferTo(response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new BusinessException("下载失败");
        }
    }

    public Result<String> upload(@RequestParam("file") MultipartFile file, String userId) {

        UserContext.setUserId(userId);
        try {
            // 上传文件到oss
            String fileName = minioService.uploadFile(file,"ai-helper-knowledge");

            // 保存信息到mysql
            tbKnowledgeService.save(new TbKnowledge("ai-helper-knowledge",fileName));

            // 矢量化到chroma
            this.saveToChroma(file,fileName);

            return Result.success("上传成功");
        } catch (Exception e) {
            log.error(e.getMessage());
            return Result.fail("上传失败");
        }
    }

    private void saveToChroma(MultipartFile file,String fileName) throws Exception{
        // 读取文件内容
        String fileText = ReadFilesUtil.extractText(file);
        log.info("读取文件内容{}",fileText);

        // 文档切割器 按照换行拆
        DocumentByParagraphSplitter documentByParagraphSplitter = new DocumentByParagraphSplitter(500, 50);

        ChromaEmbeddingStore chromaEmbeddingStore = ChromaEmbeddingStoreFactory.getChromaEmbeddingStore("knowledge");
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingStore(chromaEmbeddingStore)
                .embeddingModel(openAiEmbeddingModel)
                .documentSplitter(documentByParagraphSplitter)
        .build();

        // 将文本封装成 Document
        Document doc = Document.from(fileText, Metadata.from(Map.of("createBy",UserContext.getUserId(),
                "createTime",System.currentTimeMillis(),"fileName",fileName)));

        ingestor.ingest(doc);

//            PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:*资料.txt");
//            // 获取文档
//            List<Document> documents = loadDocuments("D:\\project\\ideaworkspace\\ai-helper\\src\\main\\resources",pathMatcher);
//        List<Document> documents = loadDocuments(toPath("documents/"), glob("*.txt"));

            // 使用文档分割器，把每个 Document 切成小块（每块500字符，重叠50）按照换行标点符号拆
//        RecursiveDocumentSplitter splitter = new RecursiveDocumentSplitter(500, 50);

            // 智能切割器
//        DocumentSplitter splitter = DocumentSplitters.recursive(500, 50);
    }

}
