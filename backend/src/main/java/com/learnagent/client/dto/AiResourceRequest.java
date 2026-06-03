package com.learnagent.client.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AiResourceRequest {
    private String taskId;
    private Long studentId;
    private Long courseId;
    private Long knowledgePointId;
    private String knowledgePointName;
    private Map<String, Object> profileJson;
    private List<String> resourceTypes;
    private List<ChunkInfo> courseChunks;

    @Data
    public static class ChunkInfo {
        private String chunkId;
        private String content;
        private String sourceFile;
    }
}
