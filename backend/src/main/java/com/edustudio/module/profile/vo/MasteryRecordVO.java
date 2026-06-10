package com.edustudio.module.profile.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MasteryRecordVO {

    private Long id;
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
