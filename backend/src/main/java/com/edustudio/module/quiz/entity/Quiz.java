package com.edustudio.module.quiz.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.edustudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("quiz")
public class Quiz extends BaseEntity {

    private Long userId;
    private Long spaceId;
    private Long resourceId;
    private String title;
    private String subject;
    private String difficulty;
    private Integer questionCount;
    private BigDecimal totalScore;
    private String status;
}
