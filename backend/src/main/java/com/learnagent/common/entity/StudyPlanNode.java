package com.learnagent.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("study_plan_node")
public class StudyPlanNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private Long knowledgePointId;

    private String knowledgePointName;

    private Integer nodeOrder;

    private Integer estimatedMinutes;

    private String recommendedResourceIds;

    private String reason;

    private String completionCriteria;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
