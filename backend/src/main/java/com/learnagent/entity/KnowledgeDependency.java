package com.learnagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_dependency")
public class KnowledgeDependency {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private Long fromKnowledgePointId;

    private Long toKnowledgePointId;

    private String relation;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
