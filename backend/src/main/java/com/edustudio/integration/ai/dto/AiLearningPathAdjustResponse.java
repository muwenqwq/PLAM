package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class AiLearningPathAdjustResponse {

    private Boolean success;

    private String summary;

    @JsonProperty("adjusted_items")
    private List<AiLearningPathItemDTO> adjustedItems;
}
