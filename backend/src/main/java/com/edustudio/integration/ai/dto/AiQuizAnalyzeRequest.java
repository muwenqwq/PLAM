package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiQuizAnalyzeRequest {

    @JsonProperty("model_config")
    private AiModelConfigDTO modelConfig;

    @JsonProperty("quiz_title")
    private String quizTitle;

    private BigDecimal score;

    @JsonProperty("total_score")
    private BigDecimal totalScore;

    @JsonProperty("weak_points")
    private List<String> weakPoints;

    private List<Map<String, Object>> answers;
}
