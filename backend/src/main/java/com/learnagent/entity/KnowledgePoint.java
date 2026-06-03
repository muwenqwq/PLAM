package com.learnagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(autoResultMap = true)
public class KnowledgePoint {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private String pointKey;

    private String name;

    private String pointType;

    private Integer difficulty;

    private String chapterFile;


    private String learningOutcomes;

    private String demoPriority;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
