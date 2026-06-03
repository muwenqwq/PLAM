package com.learnagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName(autoResultMap = true)
public class Resource {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long courseId;

    private Long knowledgePointId;

    private String taskId;

    private String resourceType;

    private String title;

    private String content;

    private String format;

    private BigDecimal qualityScore;

    private String status;


    private String sourcesJson;


    private String reviewJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
