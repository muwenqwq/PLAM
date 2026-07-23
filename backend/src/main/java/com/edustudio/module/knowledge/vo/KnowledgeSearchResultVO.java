package com.edustudio.module.knowledge.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class KnowledgeSearchResultVO {

    private String query;
    private String answerMarkdown;
    private List<Item> results;

    @Data
    @Builder
    public static class Item {
        private Long fileId;
        private String source;
        private String sourceFileName;
        private Integer chunkIndex;
        private String chunkText;
        private BigDecimal score;
        private String retrievalMode;
        private JsonNode metadata;
    }
}