package com.learnagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName(autoResultMap = true)
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
}
