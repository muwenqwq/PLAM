package com.edustudio.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.edustudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_file")
public class KnowledgeFile extends BaseEntity {

    private Long userId;
    private Long spaceId;
    private String originalName;
    private String storagePath;
    private String fileType;
    private Long fileSize;
    private String parserStatus;
    private Integer chunkCount;
    private String checksum;
    private String errorMessage;
    private String status;
}
