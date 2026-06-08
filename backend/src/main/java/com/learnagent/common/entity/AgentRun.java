package com.learnagent.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(autoResultMap = true)
public class AgentRun {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;

    private Long studentId;

    private Long courseId;

    private String agentName;

    private String agentRole;

    private String inputSummary;

    private String outputSummary;

    private String modelName;

    private String status;

    private Integer latencyMs;

    private String errorMessage;


    private String traceJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
