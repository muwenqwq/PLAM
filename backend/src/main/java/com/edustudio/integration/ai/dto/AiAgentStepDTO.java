package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class AiAgentStepDTO {

    @JsonProperty("agent_name")
    private String agentName;

    @JsonProperty("step_order")
    private Integer stepOrder;

    @JsonProperty("step_type")
    private String stepType;

    @JsonProperty("execution_status")
    private String executionStatus;

    @JsonProperty("input_json")
    private JsonNode inputJson;

    @JsonProperty("output_summary")
    private String outputSummary;

    @JsonProperty("result_json")
    private JsonNode resultJson;

    @JsonProperty("error_message")
    private String errorMessage;
}
