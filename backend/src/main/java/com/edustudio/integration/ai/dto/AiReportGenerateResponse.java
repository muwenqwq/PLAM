package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class AiReportGenerateResponse {

    private Boolean success;

    private String title;

    private String summary;

    @JsonProperty("suggestion_text")
    private String suggestionText;

    @JsonProperty("report_json")
    private Map<String, Object> reportJson;

    @JsonProperty("chart_data_json")
    private Map<String, Object> chartDataJson;
}
