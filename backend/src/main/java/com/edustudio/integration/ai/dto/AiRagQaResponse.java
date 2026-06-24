package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AiRagQaResponse {

    private Boolean success;

    @JsonProperty("answer_markdown")
    private String answerMarkdown;

    private List<AiRagChunkDTO> citations;

    @JsonProperty("result_json")
    private Map<String, Object> resultJson;
}
