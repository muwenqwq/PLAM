package com.edustudio.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.edustudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_chunk")
public class KnowledgeChunk extends BaseEntity {

    private Long knowledgeFileId;
    private Long userId;
    private Long spaceId;
    private Integer chunkIndex;
    private String contentText;
    private String contentHash;
    private Integer tokenCount;
    private String metadata;
    private String embeddingRef;
    private String status;
}
