package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

@Data
public class AiAgentRunResponse {

    private Boolean success;

    @JsonProperty("execution_status")
    private String executionStatus;

    @JsonProperty("output_summary")
    private String outputSummary;

    @JsonProperty("result_json")
    private JsonNode resultJson;

    private List<AiAgentStepDTO> steps;

    private List<AiResourceDTO> resources;

    @JsonProperty("error_message")
    private String errorMessage;
}
