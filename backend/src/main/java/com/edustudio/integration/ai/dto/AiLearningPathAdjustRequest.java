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
public class AiLearningPathAdjustRequest {

    @JsonProperty("model_config")
    private AiModelConfigDTO modelConfig;

    @JsonProperty("path_title")
    private String pathTitle;

    @JsonProperty("current_progress")
    private BigDecimal currentProgress;

    private String reason;

    private List<Map<String, Object>> items;
}
