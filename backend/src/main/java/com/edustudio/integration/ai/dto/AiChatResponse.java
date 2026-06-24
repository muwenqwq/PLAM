package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class AiChatResponse {

    @JsonProperty("provider_type")
    private String providerType;

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("reply_markdown")
    private String replyMarkdown;

    @JsonProperty("reply_json")
    private JsonNode replyJson;

    @JsonProperty("token_count")
    private Integer tokenCount;
}
