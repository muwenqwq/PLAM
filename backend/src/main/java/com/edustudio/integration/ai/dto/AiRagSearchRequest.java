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
public class AiRagSearchRequest {

    private String query;

    @JsonProperty("file_ids")
    private List<Long> fileIds;

    @JsonProperty("top_k")
    private Integer topK;

    private Map<String, Object> filters;
}
