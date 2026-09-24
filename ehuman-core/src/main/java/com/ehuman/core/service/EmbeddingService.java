package com.ehuman.core.service;

import com.ehuman.dao.entity.TbUserModelConfigDo;
import com.ehuman.dao.enums.LlmTypeEnum;
import com.ehuman.dao.service.TbUserModelConfigDoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 类的描述：embedding service
 * 作者：huhanzhao
 * 创建时间：2026/8/10
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final TbUserModelConfigDoService tbUserModelConfigDoService;

    public float[] embeddingText(Long userId, String text){
        TbUserModelConfigDo modelConfigDo = tbUserModelConfigDoService.getByUserIdAndNodeNum(userId, LlmTypeEnum.EMBEDDING);
        if (modelConfigDo == null){
            log.info("没有向量模型跳过");
            return null;
        }
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(modelConfigDo.getApiKey())
                .baseUrl(modelConfigDo.getBaseUrl()).build();

        OpenAiEmbeddingOptions openAiEmbeddingOptions = OpenAiEmbeddingOptions.builder()
                .model(modelConfigDo.getModel()).dimensions(1024)
                .build();
        OpenAiEmbeddingModel openAiEmbeddingModel = new OpenAiEmbeddingModel(openAiApi, MetadataMode.NONE,openAiEmbeddingOptions);

        EmbeddingRequest embeddingRequest = new EmbeddingRequest(List.of(text),openAiEmbeddingOptions);
        EmbeddingResponse response = openAiEmbeddingModel.call(embeddingRequest);
        float[] output = response.getResult().getOutput();
        return output;
    }
}
