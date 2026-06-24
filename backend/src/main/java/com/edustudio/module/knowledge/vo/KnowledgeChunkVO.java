package com.edustudio.module.knowledge.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeChunkVO {

    private Long id;
    private Long knowledgeFileId;
    private Integer chunkIndex;
    private String contentText;
    private String contentHash;
    private Integer tokenCount;
    private JsonNode metadata;
    private String embeddingRef;
    private String status;
}
