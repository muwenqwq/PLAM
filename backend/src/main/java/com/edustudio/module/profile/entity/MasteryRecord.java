package com.edustudio.module.profile.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.edustudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mastery_record")
public class MasteryRecord extends BaseEntity {

    private Long userId;
    private Long spaceId;
    private String knowledgePoint;
    private String subject;
    private BigDecimal masteryLevel;
    private BigDecimal weaknessLevel;
    private Long lastQuizId;
    private BigDecimal lastScore;
    private Integer reviewCount;
    private String status;
}
