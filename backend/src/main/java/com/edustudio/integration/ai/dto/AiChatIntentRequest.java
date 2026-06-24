package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatIntentRequest {

    @JsonProperty("model_config")
    private AiModelConfigDTO modelConfig;

    private String message;

    private String subject;
}
