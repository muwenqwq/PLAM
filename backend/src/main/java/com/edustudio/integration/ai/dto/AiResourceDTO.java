package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AiResourceDTO {

    @JsonProperty("resource_type")
    private String resourceType;

    private String title;

    private String subject;

    @JsonProperty("knowledge_points")
    private List<String> knowledgePoints;

    @JsonProperty("content_markdown")
    private String contentMarkdown;

    @JsonProperty("content_json")
    private JsonNode contentJson;

    @JsonProperty("output_summary")
    private String outputSummary;

    @JsonProperty("quality_score")
    private BigDecimal qualityScore;
}
