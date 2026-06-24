package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class AiRagIndexResponse {

    private Boolean success;

    @JsonProperty("parser_status")
    private String parserStatus;

    @JsonProperty("chunk_count")
    private Integer chunkCount;

    private List<AiRagChunkDTO> chunks;
}
