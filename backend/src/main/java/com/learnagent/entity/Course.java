package com.learnagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(autoResultMap = true)
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String courseCode;

    private String courseName;

    private String description;


    private String stageScope;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
