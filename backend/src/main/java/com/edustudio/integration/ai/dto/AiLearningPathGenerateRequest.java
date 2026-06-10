package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiLearningPathGenerateRequest {

    @JsonProperty("model_config")
    private AiModelConfigDTO modelConfig;

    private String subject;

    private String goal;

    @JsonProperty("knowledge_points")
    private List<String> knowledgePoints;

    private Integer days;

    private JsonNode preference;
}
