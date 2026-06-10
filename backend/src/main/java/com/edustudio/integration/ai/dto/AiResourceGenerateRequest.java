package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiResourceGenerateRequest {

    @JsonProperty("model_config")
    private AiModelConfigDTO modelConfig;

    private String title;

    private String subject;

    @JsonProperty("resource_type")
    private String resourceType;

    @JsonProperty("input_params")
    private JsonNode inputParams;
}
