package com.learnagent.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("profile_snapshot")
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

    @TableField(exist = false)  // 表暂无此列
    private LocalDateTime updatedAt;
}
