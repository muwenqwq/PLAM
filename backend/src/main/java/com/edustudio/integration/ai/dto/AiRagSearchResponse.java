package com.edustudio.integration.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiRagSearchResponse {

    private Boolean success;

    private String query;

    private List<AiRagChunkDTO> results;
}
