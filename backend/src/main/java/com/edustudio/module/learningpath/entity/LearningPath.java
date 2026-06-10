package com.edustudio.module.learningpath.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.edustudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("learning_path")
public class LearningPath extends BaseEntity {

    private Long userId;
    private Long spaceId;
    private String title;
    private String goal;
    private String subject;
    private String planJson;
    private BigDecimal progressRate;
    private LocalDate startDate;
    private LocalDate targetDate;
    private String status;
}
