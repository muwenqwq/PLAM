package com.learnagent.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("assessment_result")
public class AssessmentResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long courseId;

    private Long knowledgePointId;

    private Long resourceId;

    private String taskId;

    private String quizTitle;

    private BigDecimal score;

    private BigDecimal totalScore;

    private String masteryDeltaJson;

    private String detailsJson;

    private LocalDateTime submittedAt;

    @TableField(exist = false)  // 表暂无此列
    private LocalDateTime createdAt;

    @TableField(exist = false)  // 表暂无此列
    private LocalDateTime updatedAt;
}
