package com.learnagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(autoResultMap = true)
public class TutorMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    private String taskId;

    private Long studentId;

    private Long courseId;

    private String messageRole;

    private String content;


    private String sourcesJson;


    private String suggestedResourceIds;

    private String safetyStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
