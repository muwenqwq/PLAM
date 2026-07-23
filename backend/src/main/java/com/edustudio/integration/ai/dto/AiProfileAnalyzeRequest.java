package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiProfileAnalyzeRequest {

    @JsonProperty("model_config")
    private AiModelConfigDTO modelConfig;

    @JsonProperty("current_profile")
    private Map<String, Object> currentProfile;

    private String source;

    private String subject;

    @JsonProperty("knowledge_points")
    private List<String> knowledgePoints;

    @JsonProperty("weak_points")
    private List<String> weakPoints;

    private Map<String, Object> evidence;
}
