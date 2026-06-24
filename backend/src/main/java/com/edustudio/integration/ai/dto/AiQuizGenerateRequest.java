package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiQuizGenerateRequest {

    @JsonProperty("model_config")
    private AiModelConfigDTO modelConfig;

    private String subject;

    private String title;

    @JsonProperty("knowledge_points")
    private List<String> knowledgePoints;

    @JsonProperty("question_count")
    private Integer questionCount;

    private String difficulty;

    @JsonProperty("question_type")
    private String questionType;
}
