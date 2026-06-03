package com.learnagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(autoResultMap = true)
public class AiTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;

    private Long studentId;

    private Long courseId;

    private String taskType;


    private String requestJson;


    private String responseJson;

    private String status;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
