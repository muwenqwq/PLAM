package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {

    @JsonProperty("model_config")
    private AiModelConfigDTO modelConfig;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("conversation_id")
    private Long conversationId;

    @JsonProperty("space_id")
    private Long spaceId;

    private String subject;

    private String message;

    private List<Map<String, String>> history;

    private Map<String, Object> profile;

    private Map<String, Object> preference;
}
