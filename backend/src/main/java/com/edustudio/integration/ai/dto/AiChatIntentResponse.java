package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiChatIntentResponse {

    @JsonProperty("intent_type")
    private String intentType;

    private BigDecimal confidence;

    private String subject;

    private JsonNode slots;
}
