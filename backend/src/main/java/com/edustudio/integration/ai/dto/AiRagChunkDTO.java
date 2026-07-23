package com.edustudio.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class AiRagChunkDTO {

    @JsonProperty("chunk_index")
    private Integer chunkIndex;

    @JsonProperty("content_text")
    private String contentText;

    @JsonProperty("content_hash")
    private String contentHash;

    @JsonProperty("token_count")
    private Integer tokenCount;

    private Map<String, Object> metadata;

    @JsonProperty("embedding_ref")
    private String embeddingRef;

    private BigDecimal score;

    private String source;

    @JsonProperty("source_file_name")
    private String sourceFileName;

    @JsonProperty("retrieval_mode")
    private String retrievalMode;
}
