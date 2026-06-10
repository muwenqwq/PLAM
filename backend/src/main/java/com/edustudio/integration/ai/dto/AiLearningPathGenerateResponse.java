package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AiLearningPathGenerateResponse {

    private Boolean success;

    private String title;

    private String summary;

    @JsonProperty("plan_json")
    private Map<String, Object> planJson;

    private List<AiLearningPathItemDTO> items;
}
