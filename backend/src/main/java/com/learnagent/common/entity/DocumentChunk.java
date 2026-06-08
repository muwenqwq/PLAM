package com.learnagent.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("document_chunk")
public class DocumentChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private Long knowledgePointId;

    private String chunkId;

    private String content;

    private String sourceFile;

    private Integer chunkOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
