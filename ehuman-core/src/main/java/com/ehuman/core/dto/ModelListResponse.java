package com.ehuman.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 获取模型列表响应 DTO
 */
@Data
public class ModelListResponse {

    @JsonProperty("object")
    private String object;

    @JsonProperty("data")
    private List<ModelData> data;

    @Data
    public static class ModelData {
        @JsonProperty("id")
        private String id;

        @JsonProperty("object")
        private String object;

        @JsonProperty("owned_by")
        private String ownedBy;

        @JsonProperty("name")
        private String name;

        @JsonProperty("context_window")
        private Integer contextWindow;

        @JsonProperty("max_output_tokens")
        private Integer maxOutputTokens;

        @JsonProperty("input_modalities")
        private List<String> inputModalities;

        @JsonProperty("output_modalities")
        private List<String> outputModalities;

        @JsonProperty("effort")
        private Effort effort;

        @JsonProperty("api_capabilities")
        private ApiCapabilities apiCapabilities;
    }

    @Data
    public static class Effort {
        @JsonProperty("supported_levels")
        private List<String> supportedLevels;

        @JsonProperty("default_level")
        private String defaultLevel;
    }

    @Data
    public static class ApiCapabilities {
        @JsonProperty("anthropic_messages")
        private AnthropicMessages anthropicMessages;
    }

    @Data
    public static class AnthropicMessages {
        @JsonProperty("system_prompt_update")
        private String systemPromptUpdate;

        @JsonProperty("tool_update")
        private String toolUpdate;
    }
}