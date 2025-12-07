package com.helper.knowledge.controller;

import com.helper.commonservicescommon.entity.Result;
import com.helper.knowledge.service.KnowledgeService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/knowledge")
@Slf4j
public class KnowledgeManageController {

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private MinioClient minioClient;

    /**
     * 从minio中下载文件
     * @param fileName 文件名
     * @param response 相应体
     */
    @GetMapping("/download/{fileName}")
    public void download(@PathVariable(name = "fileName") String fileName,
                         HttpServletResponse response){
        // 下载文件
        knowledgeService.downloadFile(fileName,response);
    }

    /**
     * 上传文件到minio 支持doc，docx，pdf，txt
     * @param file 文件
     * @param userId userId
     * @return 统一返回对象
     */
    @PostMapping(value = "/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> upload(@RequestParam("file") MultipartFile file,@RequestHeader("X-User-Id") String userId) {
        return knowledgeService.upload(file, userId);
    }

    @GetMapping(value = "/imageTest")
    public Result<String> getImage() throws Exception{
        String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket("ai-helper-knowledge")
                        .object("Snipaste_2025-12-07_19-43-48.png")
                        .expiry(60 * 60)  // 有效期 1 小时
                        .build()
        );

        return Result.success(url);
    }
}
