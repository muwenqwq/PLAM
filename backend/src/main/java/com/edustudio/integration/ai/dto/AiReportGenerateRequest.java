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
public class AiReportGenerateRequest {

    @JsonProperty("model_config")
    private AiModelConfigDTO modelConfig;

    @JsonProperty("report_type")
    private String reportType;

    private String title;

    private Map<String, Object> overview;

    @JsonProperty("mastery_records")
    private List<Map<String, Object>> masteryRecords;

    @JsonProperty("learning_evidence")
    private Map<String, Object> learningEvidence;

    private Map<String, Object> profile;

    @JsonProperty("role_play_enabled")
    private Boolean rolePlayEnabled;

    @JsonProperty("companion_role")
    private Map<String, Object> companionRole;
}
