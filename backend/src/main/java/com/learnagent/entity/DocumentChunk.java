package com.learnagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(autoResultMap = true)
public class DocumentChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String chunkKey;

    private Long courseId;

    private Long knowledgePointId;

    private String sourceFile;

    private String title;

    private String content;

    private String embeddingStatus;

    private String vectorCollection;


    private String metadata;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
