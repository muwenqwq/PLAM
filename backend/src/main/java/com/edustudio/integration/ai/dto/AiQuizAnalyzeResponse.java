package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class AiQuizAnalyzeResponse {

    private Boolean success;

    @JsonProperty("analysis_markdown")
    private String analysisMarkdown;

    private List<String> suggestions;
}
