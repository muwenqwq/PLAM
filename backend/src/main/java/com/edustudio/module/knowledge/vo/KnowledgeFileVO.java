package com.edustudio.module.knowledge.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class KnowledgeFileVO {

    private Long id;
    private Long userId;
    private Long spaceId;
    private String originalName;
    private String storagePath;
    private String fileType;
    private Long fileSize;
    private String parserStatus;
    private Integer chunkCount;
    private String errorMessage;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<KnowledgeChunkVO> chunks;
}
