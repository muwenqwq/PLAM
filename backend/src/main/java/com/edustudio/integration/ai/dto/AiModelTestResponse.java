package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AiModelTestResponse {

    private Boolean success;

    @JsonProperty("provider_type")
    private String providerType;

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("latency_ms")
    private Integer latencyMs;

    private String message;

    @JsonProperty("sample_output")
    private String sampleOutput;
}
