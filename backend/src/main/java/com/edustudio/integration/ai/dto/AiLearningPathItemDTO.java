package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class AiLearningPathItemDTO {

    private String title;

    private String description;

    @JsonProperty("knowledge_points")
    private List<String> knowledgePoints;

    @JsonProperty("estimated_minutes")
    private Integer estimatedMinutes;

    private String difficulty;

    @JsonProperty("due_day")
    private Integer dueDay;
}
