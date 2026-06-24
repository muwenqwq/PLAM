package com.edustudio.module.learningpath.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.edustudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("learning_path_item")
public class LearningPathItem extends BaseEntity {

    private Long pathId;
    private Long userId;
    private Long spaceId;
    private Integer itemOrder;
    private String title;
    private String description;
    private Long resourceId;
    private String knowledgePoints;
    private Integer estimatedMinutes;
    private LocalDate dueDate;
    private LocalDateTime completedAt;
    private String status;
}
