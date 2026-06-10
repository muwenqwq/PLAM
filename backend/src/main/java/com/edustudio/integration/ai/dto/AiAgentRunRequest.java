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
public class AiAgentRunRequest {

    @JsonProperty("model_config")
    private AiModelConfigDTO modelConfig;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("space_id")
    private Long spaceId;

    @JsonProperty("provider_id")
    private Long providerId;

    @JsonProperty("task_type")
    private String taskType;

    private String title;

    private String subject;

    @JsonProperty("resource_type")
    private String resourceType;

    @JsonProperty("input_params")
    private JsonNode inputParams;
}
