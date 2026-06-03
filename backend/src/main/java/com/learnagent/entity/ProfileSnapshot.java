package com.learnagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(autoResultMap = true)
public class ProfileSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long courseId;

    private String taskId;


    private String profileJson;

    private String source;

    private Integer versionNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
