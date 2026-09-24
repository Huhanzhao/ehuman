package com.ehuman.core.utils;

import com.ehuman.dao.enums.LlmChatEnum;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 类的描述：提示词读取工具
 * 作者：huhanzhao
 * 创建时间：2026/8/17
 */
public class PromptReadUtil {

    public static PromptTemplate readPrompt(LlmChatEnum llmChatEnum) {
        try {
            return PromptTemplate.builder()
                    .template(Files.readString(Paths.get(llmChatEnum.getPromptTemplatePath())))
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
